-- Remoção de dependentes pelo responsável é lógica (inativação): o histórico de exames fica preservado
-- (agendamentos.paciente_id é ON DELETE RESTRICT, então apagar a linha não seria possível).
ALTER TABLE pacientes ADD COLUMN ativo BOOLEAN NOT NULL DEFAULT TRUE;

-- O CPF continua único, mas só entre pacientes ativos: quem foi removido pode ser cadastrado de novo.
DROP INDEX IF EXISTS idx_pacientes_cpf_unico;
CREATE UNIQUE INDEX idx_pacientes_cpf_unico ON pacientes (cpf) WHERE cpf IS NOT NULL AND ativo;
