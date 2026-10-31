--
-- Schema do sistema de agendamento de exames
-- Incorpora as decisões definidas:
--   - public_id (UUID) exposto na API, id (BIGINT) só uso interno
--   - cargo_id único por usuário (sem multi-cargo)
--   - pacientes com PK própria + usuario_responsavel_id (permite agendar para
--     dependentes sem login próprio)
--   - cancelamento/recusa simplificados em observacao + alterado_por + alterado_em
--   - data_opcao_1/2/3 fixas (validação de quantidade fica no backend)
--   - sem histórico de reenvio (solicitacao_anterior_id) — fora de escopo por ora
--

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- =========================================================
-- 1. cargos — papéis do sistema
-- =========================================================
CREATE TABLE cargos (
    id      BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nome    VARCHAR(50) NOT NULL UNIQUE  -- ROLE_PACIENTE, ROLE_RECEPCIONISTA, ROLE_MEDICO, ROLE_ADMIN...
);

-- =========================================================
-- 2. enderecos
-- =========================================================
CREATE TABLE enderecos (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    public_id    UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    cep          VARCHAR(8)   NOT NULL,
    logradouro   VARCHAR(150) NOT NULL,
    numero       VARCHAR(20)  NOT NULL,
    complemento  VARCHAR(100),
    bairro       VARCHAR(100) NOT NULL,
    cidade       VARCHAR(100) NOT NULL,
    estado       VARCHAR(2)   NOT NULL
);

-- =========================================================
-- 3. usuarios — quem faz LOGIN no sistema
--    (paciente adulto titular, dependente NÃO entra aqui, atendente,
--     médico, admin)
-- =========================================================
CREATE TABLE usuarios (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    public_id       UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    nome            VARCHAR(150) NOT NULL,
    cpf             VARCHAR(11)  NOT NULL UNIQUE,
    email           VARCHAR(150) NOT NULL UNIQUE,
    senha_hash      VARCHAR(255) NOT NULL,
    telefone        VARCHAR(20)  NOT NULL,
    data_nascimento DATE NOT NULL,
    ativo           BOOLEAN NOT NULL DEFAULT TRUE,
    data_criacao    TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- RESTRICT: nao deixa apagar um cargo/endereco que ainda esta em uso
    cargo_id        BIGINT NOT NULL REFERENCES cargos(id) ON DELETE RESTRICT,
    endereco_id     BIGINT NOT NULL REFERENCES enderecos(id) ON DELETE RESTRICT
);

-- =========================================================
-- 4. pacientes — quem PODE SER ATENDIDO (com ou sem login próprio)
--    PK própria, não herda de usuarios. usuario_responsavel_id é
--    sempre quem tem a conta e faz o agendamento.
-- =========================================================
CREATE TABLE pacientes (
    id                      BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    public_id               UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    -- RESTRICT: nao deixa apagar um usuario que ainda tem pacientes vinculados
    -- (o app deve inativar o usuario via campo "ativo", nao apagar de verdade)
    usuario_responsavel_id  BIGINT NOT NULL REFERENCES usuarios(id) ON DELETE RESTRICT,
    nome                    VARCHAR(150) NOT NULL,
    cpf                     VARCHAR(11),  -- opcional: dependente pode nao ter ainda
    data_nascimento         DATE NOT NULL,
    parentesco              VARCHAR(50) NOT NULL,
    convenio                VARCHAR(80) NOT NULL,
    numero_carteirinha      VARCHAR(50),
    tipo_sanguineo          VARCHAR(3),
    alergias                TEXT,
    CONSTRAINT chk_pacientes_parentesco CHECK (
        parentesco IN ('TITULAR', 'FILHO', 'CONJUGE', 'PAI_MAE', 'OUTRO')
    )
);

-- cpf só precisa ser único quando preenchido
CREATE UNIQUE INDEX idx_pacientes_cpf_unico ON pacientes (cpf) WHERE cpf IS NOT NULL;

-- =========================================================
-- 5. medicos — extensão 1:1 de usuarios (médico sempre tem login)
-- =========================================================
CREATE TABLE medicos (
    -- CASCADE: a linha de medico nao tem sentido sem o usuario (é so extensao dele)
    usuario_id      BIGINT PRIMARY KEY REFERENCES usuarios(id) ON DELETE CASCADE,
    crm             VARCHAR(20) NOT NULL,
    uf_crm          VARCHAR(2)  NOT NULL,
    especialidade   VARCHAR(100) NOT NULL,
    UNIQUE (crm, uf_crm)
);

-- =========================================================
-- 6. exames — catálogo
-- =========================================================
CREATE TABLE exames (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    public_id   UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    nome        VARCHAR(100) NOT NULL,
    descricao   TEXT,
    ativo       BOOLEAN NOT NULL DEFAULT TRUE
);

-- =========================================================
-- 7. agendamentos — core do sistema
-- =========================================================
CREATE TABLE agendamentos (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    public_id         UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),

    -- RESTRICT nos vinculos que formam o historico medico/administrativo:
    -- nao pode sumir um paciente, solicitante ou exame que tem agendamento associado
    paciente_id       BIGINT NOT NULL REFERENCES pacientes(id) ON DELETE RESTRICT,
    solicitante_id    BIGINT NOT NULL REFERENCES usuarios(id) ON DELETE RESTRICT,
    exame_id          BIGINT NOT NULL REFERENCES exames(id) ON DELETE RESTRICT,

    -- SET NULL nos vinculos opcionais: perder o medico/atendente nao deve apagar o agendamento
    medico_id         BIGINT REFERENCES medicos(usuario_id) ON DELETE SET NULL,
    atendente_id      BIGINT REFERENCES usuarios(id) ON DELETE SET NULL,

    status            VARCHAR(20) NOT NULL DEFAULT 'PENDENTE',

    data_opcao_1      TIMESTAMPTZ NOT NULL,
    data_opcao_2      TIMESTAMPTZ NOT NULL,
    data_opcao_3      TIMESTAMPTZ NOT NULL,
    data_confirmada   TIMESTAMPTZ,  -- preenchida ao confirmar

    -- campo unico para motivo, cobre tanto recusa quanto cancelamento
    observacao        TEXT,
    alterado_por      BIGINT REFERENCES usuarios(id) ON DELETE SET NULL,
    alterado_em       TIMESTAMPTZ,

    data_criacao      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_agendamentos_status CHECK (
        status IN ('PENDENTE', 'CONFIRMADO', 'RECUSADO', 'CONCLUIDO', 'CANCELADO')
    )
);

-- =========================================================
-- Índices auxiliares (consultas mais comuns)
-- =========================================================
CREATE INDEX idx_agendamentos_paciente    ON agendamentos (paciente_id);
CREATE INDEX idx_agendamentos_solicitante ON agendamentos (solicitante_id);
CREATE INDEX idx_agendamentos_medico      ON agendamentos (medico_id);
CREATE INDEX idx_agendamentos_status      ON agendamentos (status);
CREATE INDEX idx_pacientes_responsavel    ON pacientes (usuario_responsavel_id);

-- Impede dois agendamentos CONFIRMADOS pro mesmo medico no mesmo horario
-- (o WHERE deixa varios PENDENTE concorrerem pelo mesmo horario normalmente,
--  so trava quando os dois tentam ficar CONFIRMADO ao mesmo tempo)
CREATE UNIQUE INDEX idx_agendamentos_medico_horario_unico
    ON agendamentos (medico_id, data_confirmada)
    WHERE status = 'CONFIRMADO';

-- Impede duas linhas "TITULAR" pro mesmo usuario responsavel
CREATE UNIQUE INDEX idx_pacientes_titular_unico
    ON pacientes (usuario_responsavel_id)
    WHERE parentesco = 'TITULAR';
