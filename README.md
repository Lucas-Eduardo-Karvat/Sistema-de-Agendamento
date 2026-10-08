# RELATÓRIO TÉCNICO COMPLETO E GUIA DE INTEGRAÇÃO FRONTEND × BACKEND

---

## 1. EVOLUÇÃO E ESTADO ATUAL DO PROJETO

O sistema trata-se de um **Sistema de Agendamento Hospitalar / Exames (Agendamento API)** desenvolvido em **Java 21** utilizando o framework **Spring Boot 3.x**. O objetivo principal da aplicação é gerenciar o fluxo completo de atendimento: desde a autenticação e cadastro de usuários/pacientes até o agendamento de exames e consultas, controle de permissões por cargo, gerenciamento de cargos/permissões e log de solicitações assíncronas.

### Evolução Histórica das Implementações:

1. **Modelagem de Domínio & Persistência Relacional**:
* Criação das entidades JPA (`Usuario`, `Cargo`, `Endereco`, `Exame`, `Agendamento`) com mapeamentos e annotations de validação (`Jakarta Validation`).
* Mapeamento de chaves primárias utilizando UUIDs nativos gerados via banco (`public_id`) combinados com chaves primárias sequenciais numéricas (`id`) para otimização de joins internos no banco PostgreSQL.


2. **Camada de Transferência de Dados (DTOs)**:
* Implementação da separação de responsabilidades entre modelo de banco e payloads da API via Records Java (`UsuarioRequestDTO`, `UsuarioResponseDTO`, `LoginDTO`, `TokenDTO`).


3. **Persistência com Spring Data JPA**:
* Construção dos Repositories com consultas customizadas (`findByEmail`, `findByCpf`, `findByPublicId`) para validação de unicidade e busca eficiente.


4. **Segurança, Hashing de Senhas e Autenticação JWT**:
* Migração de senhas em texto puro para criptografia forte via `BCryptPasswordEncoder`.
* Implementação do serviço de tokens JWT (`TokenService`) utilizando a biblioteca Auth0 (`com.auth0:java-jwt`), configurado para gerar e validar tokens com Subject (e-mail), Public ID e Cargo/Role.
* Construção do serviço de autenticação (`AuthService`) responsável por comparar senhas criptografadas e emitir o token de sessão.


5. **Tratamento Global de Exceções**:
* Criação do `GlobalExceptionHandler` (`@RestControllerAdvice`) para interceptar exceções como `IllegalArgumentException` ou falhas de credenciais e convertê-las de erro padrão `500 Internal Server Error` para `401 Unauthorized` ou `400 Bad Request` estruturados em formato JSON padronizado.



---

## 2. ARQUITETURA COMPLETA DA APLICAÇÃO

O projeto adota uma arquitetura em camadas bem definida (*Layered Architecture*), isolando rigorosamente a regra de negócio do acesso aos dados e da exposição dos endpoints REST.

### Fluxo de Ida (Requisição HTTP do Frontend ao Banco de Dados):

```
+------------------+     HTTP Request (POST/GET) + JSON     +-----------------------+
|  Frontend Client | -------------------------------------> | Controller (REST)     |
+------------------+                                        +-----------------------+
                                                                        |
                                                                Passa Request DTO
                                                                        v
+------------------+        Converte/Mapeia Entidade        +-----------------------+
| Database (Postgre| <------------------------------------- | Service (Regra)       |
+------------------+                                        +-----------------------+
        ^                                                               |
        |                                                       Chama métodos JPA
        +--------------------------------------------------------------+
                                Repository JPA

```

1. **Frontend Client**: Dispara uma requisição HTTP via `fetch`/`axios` contendo Headers (e.g., `Authorization: Bearer <token>`) e um Body em JSON.
2. **Controller Layer (`@RestController`)**: Intercepta a requisição na URL mapeada, executa a validação sintática das anotações do Bean Validation (`@Valid`) e desempacota o payload em um **Request DTO**.
3. **DTO (Data Transfer Object)**: Garante que dados indesejados não entrem no sistema e que os tipos de dados estejam corretos.
4. **Service Layer (`@Service`)**: Recebe os dados do DTO, executa as validações de regra de negócio (ex.: verificação se CPF ou e-mail já existem, hash de senha via BCrypt, geração de token JWT) e converte/popula a Entidade JPA.
5. **Repository Layer (`@Repository / JpaRepository`)**: Executa os comandos SQL/JPQL traduzidos pelo Hibernate para interagir com o PostgreSQL.
6. **Banco de Dados (PostgreSQL)**: Garante a persistência e integridade referencial.

---

### Fluxo de Volta (Resposta do Banco de Dados ao Frontend):

```
+------------------+        Retorna Entidade / Tupla        +-----------------------+
| Database (Postgre| -------------------------------------> | Repository JPA        |
+------------------+                                        +-----------------------+
                                                                        |
                                                              Retorna objeto Entidade
                                                                        v
+------------------+         Mapeia Entidade -> DTO         +-----------------------+
|  Frontend Client | <------------------------------------- | Service Layer         |
+------------------+                                        +-----------------------+
        ^                                                               |
        |                                                      Envia Response DTO
        +---------------------------------------------------------------+
                             Controller (200/201 HTTP)

```

1. **Database**: Retorna a tupla persistida/consultada.
2. **Repository**: Converte a tupla na instância da **Entidade JPA**.
3. **Service**: Transforma a Entidade JPA em um **Response DTO** (ocultando campos sensíveis como a hash da senha, chave primária sequencial interna `id`, etc., mantendo apenas o `usuarioPublicId` UUID).
4. **Controller**: Embala o Response DTO em um `ResponseEntity` HTTP com o código de status adequado (`200 OK`, `201 Created`, `401 Unauthorized`).
5. **JSON / Frontend**: O framework serializa o DTO em JSON e entrega ao cliente HTTP frontend.

---

## 3. DOCUMENTAÇÃO TÉCNICA DO BANCO DE DADOS

O sistema utiliza o PostgreSQL. As chaves primárias expostas ao mundo externo utilizam o padrão UUID (mapeadas nos endpoints como `publicId`), prevenindo ataques de enumeração direta e isolando as chaves numéricas sequenciais (`id`) internamente.

### Estrutura das Tabelas

#### Tabela `cargos`

Abrasiva para perfis e permissões do sistema (ex.: `PACIENTE`, `MEDICO`, `ADMINISTRADOR`, `RECEPCIONISTA`).

* **`id`** (`BIGINT` / `SERIAL`): Chave Primária Interna, Not Null.
* **`nome`** (`VARCHAR(50)`): Nome da role/cargo, Not Null, Unique.

#### Tabela `enderecos`

Armazena a localização física/residencial vinculada ao usuário.

* **`id`** (`BIGINT` / `SERIAL`): Chave Primária Interna, Not Null.
* **`cep`** (`VARCHAR(8)`): CEP sem traço, Not Null.
* **`logradouro`** (`VARCHAR(255)`): Rua/Avenida, Not Null.
* **`numero`** (`VARCHAR(20)`): Número residencial/comercial, Not Null.
* **`complemento`** (`VARCHAR(100)`): Nullable.
* **`bairro`** (`VARCHAR(100)`): Not Null.
* **`cidade`** (`VARCHAR(100)`): Not Null.
* **`estado`** (`VARCHAR(2)`): UF (Ex: `SC`, `SP`), Not Null.

#### Tabela `usuarios`

Entidade central do sistema de autenticação e identificação de pessoas.

* **`id`** (`BIGINT` / `SERIAL`): Chave Primária Interna, Not Null.
* **`public_id`** (`UUID`): Chave Primária Pública exposta ao Frontend, Not Null, Unique, Gerada Automaticamente (`uuid_generate_v4()`).
* **`nome`** (`VARCHAR(150)`): Nome completo do usuário, Not Null.
* **`cpf`** (`VARCHAR(11)`): CPF apenas números, Not Null, Unique.
* **`email`** (`VARCHAR(150)`): Endereço de e-mail de login, Not Null, Unique.
* **`senha`** (`VARCHAR(255)`): String contendo a hash BCrypt (`$2a$10$...`), Not Null.
* **`telefone`** (`VARCHAR(20)`): Telefone/WhatsApp, Not Null.
* **`data_nascimento`** (`DATE`): Data de nascimento (`YYYY-MM-DD`), Not Null.
* **`cargo_id`** (`BIGINT`): **FK** -> `cargos(id)`, Not Null.
* **`endereco_id`** (`BIGINT`): **FK** -> `enderecos(id)`, Unique, Nullable/Not Null dependendo do fluxo (Cascade ALL).

---

### Mapeamento dos Relacionamentos (Visual)

```
+--------------------+            +--------------------+
|      cargos        |            |     enderecos      |
+--------------------+            +--------------------+
| id (PK)            |            | id (PK)            |
| nome               |            | cep, logradouro... |
+--------------------+            +--------------------+
          ^                                ^
          | (1:N)                          | (1:1 CASCADE)
          |                                |
+------------------------------------------------------+
|                      usuarios                        |
+------------------------------------------------------+
| id (PK Interna)                                      |
| public_id (UUID Exposto ao Frontend)                |
| nome, cpf, email, senha, telefone, data_nascimento   |
| cargo_id (FK -> cargos.id)                           |
| endereco_id (FK -> enderecos.id)                     |
+------------------------------------------------------+

```

#### Impacto dos Relacionamentos no Frontend:

1. **Endereço Embutido**: Ao cadastrar um usuário via `POST /api/usuarios`, o frontend **não** cria o endereço separadamente. O JSON do usuário deve conter o objeto `"endereco": { ... }` embutido. O Hibernate grava o endereço primeiro e associa a FK automaticamente (`CascadeType.ALL`).
2. **Cargo via ID ou Nome**: O cadastro aceita a propriedade `cargoId`. O frontend deve saber qual o ID do cargo desejado (por padrão, `1` ou o ID relativo ao perfil de Paciente). Na resposta do backend, o cargo é retornado desnormalizado como uma String formatada (`"cargo": "PACIENTE"`).

---

## 4. ENTIDADES JPA E DTOs (DATA TRANSFER OBJECTS)

### Entidades

#### 1. `Usuario` (`@Entity`, `@Table(name = "usuarios")`)

* Contém a anotação `@PrePersist` para inicialização automática do UUID no campo `publicId = UUID.randomUUID()`.
* Possui `@OneToOne(cascade = CascadeType.ALL)` com `Endereco`.
* Possui `@ManyToOne` com `Cargo`.

#### 2. `Endereco` (`@Entity`, `@Table(name = "enderecos")`)

* Atributos padrão de endereço com mapeamento direto das colunas.

#### 3. `Cargo` (`@Entity`, `@Table(name = "cargos")`)

* Contém ID e o nome do cargo (enum ou string no banco).

---

### DTOs do Projeto

Os DTOs são declarados utilizando **Java Records** para imutabilidade e concisão sintática.

#### `UsuarioRequestDTO` (Payload enviado ao cadastrar usuário)

```java
public record UsuarioRequestDTO(
    @NotBlank String nome,
    @NotBlank @CPF String cpf,
    @NotBlank @Email String email,
    @NotBlank @Size(min = 6) String senha,
    @NotBlank String telefone,
    @NotNull LocalDate dataNascimento,
    @NotNull Long cargoId,
    @NotNull @Valid EnderecoRequestDTO endereco
) {}

```

* **Campos Obrigatórios**: Todos (`nome`, `cpf`, `email`, `senha`, `telefone`, `dataNascimento`, `cargoId`, `endereco`).
* **Validações Ativas**: CPF válido, e-mail com sintaxe válida, senha com mínimo de 6 caracteres.

#### `UsuarioResponseDTO` (Payload retornado pela API)

```java
public record UsuarioResponseDTO(
    UUID publicId,
    String nome,
    String cpf,
    String email,
    String telefone,
    LocalDate dataNascimento,
    String cargo,
    EnderecoResponseDTO endereco
) {}

```

* **Nota de Segurança**: A hash da senha e o ID sequencial numérico do banco de dados são omitidos intencionalmente.

#### `LoginDTO` (Payload do Login)

```java
public record LoginDTO(
    @NotBlank String login, // Aceita Email ou CPF
    @NotBlank String senha
) {}

```

#### `TokenDTO` (Resposta da Autenticação com Sucesso)

```java
public record TokenDTO(
    String token,
    String tipo,             // Sempre "Bearer"
    UUID usuarioPublicId,
    String nome,
    String cargo
) {}

```

---

## 5. CONTROLLERS E CATALOGAÇÃO COMPLETA DOS ENDPOINTS

### Base URL: `http://localhost:8080` (Ambiente Local)

---

### Endpoint 1: Autenticação / Login

* **Método HTTP**: `POST`
* **URL**: `/api/auth/login`
* **Autenticação Necessária**: Não (Pública).
* **Headers**: `Content-Type: application/json`

#### Body Enviado pelo Frontend (Request):

```json
{
  "login": "lucas@email.com",
  "senha": "senhaSegura123"
}

```

*Observação: O campo `login` pode receber o E-mail ou o CPF sem pontuação.*

#### Resposta do Backend em Caso de Sucesso (`200 OK`):

```json
{
  "token": "eyJhbGciOiJIUzM4NCJ9.eyJzdWIiOiJsdWNhc0BlbWFpbC5jb20iLCJwdWJsaWNJZCI6ImY4YTkyNzFkLTYwNTUtNDU4OC05YTY5LWQ4NGJjZTgzMzAzMCIsImNhcmdvIjoiUEFDSUVOVEUiLCJpYXQiOjE3OTAyMTY3MDgsImV4cCI6MTc5MDMwMzEwOH0.HlgCrYMBZkwvp82SmRSRGtYAirnYzdrI59KPGxtzLWp3VJfpln97dPvDqXtVYAsv",
  "tipo": "Bearer",
  "usuarioPublicId": "f8a9271d-6055-4588-9a69-d84bce833030",
  "nome": "Lucas Eduardo",
  "cargo": "PACIENTE"
}

```

#### Resposta em Caso de Falha de Credencial (`401 Unauthorized`):

```json
{
  "timestamp": "2026-09-27T11:54:35",
  "status": 401,
  "error": "Unauthorized",
  "message": "Usuário ou senha inválidos."
}

```

---

### Endpoint 2: Cadastro de Novo Usuário

* **Método HTTP**: `POST`
* **URL**: `/api/usuarios`
* **Autenticação Necessária**: Não (para cadastro de pacientes) / Dependente de perfil.
* **Headers**: `Content-Type: application/json`

#### Body Enviado pelo Frontend (Request):

```json
{
  "nome": "Lucas Eduardo",
  "cpf": "12345678900",
  "email": "lucas@email.com",
  "senha": "senhaSegura123",
  "telefone": "47999999999",
  "dataNascimento": "2000-01-01",
  "cargoId": 1,
  "endereco": {
    "cep": "89460000",
    "logradouro": "Rua Exemplo",
    "numero": "100",
    "complemento": "Apto 1",
    "bairro": "Centro",
    "cidade": "Três Barras",
    "estado": "SC"
  }
}

```

#### Resposta do Backend em Caso de Sucesso (`201 Created`):

```json
{
  "publicId": "f8a9271d-6055-4588-9a69-d84bce833030",
  "nome": "Lucas Eduardo",
  "cpf": "12345678900",
  "email": "lucas@email.com",
  "telefone": "47999999999",
  "dataNascimento": "2000-01-01",
  "cargo": "PACIENTE",
  "endereco": {
    "cep": "89460000",
    "logradouro": "Rua Exemplo",
    "numero": "100",
    "complemento": "Apto 1",
    "bairro": "Centro",
    "cidade": "Três Barras",
    "estado": "SC"
  }
}

```

---

### Endpoint 3: Listar Todos os Usuários

* **Método HTTP**: `GET`
* **URL**: `/api/usuarios`
* **Autenticação Necessária**: Sim (`Authorization: Bearer <token>`).
* **Headers**: `Authorization: Bearer <token>`

#### Resposta do Backend (`200 OK`):

```json
[
  {
    "publicId": "f8a9271d-6055-4588-9a69-d84bce833030",
    "nome": "Lucas Eduardo",
    "cpf": "12345678900",
    "email": "lucas@email.com",
    "telefone": "47999999999",
    "dataNascimento": "2000-01-01",
    "cargo": "PACIENTE",
    "endereco": {
      "cep": "89460000",
      "logradouro": "Rua Exemplo",
      "numero": "100",
      "complemento": "Apto 1",
      "bairro": "Centro",
      "cidade": "Três Barras",
      "estado": "SC"
    }
  }
]

```

---

### Endpoint 4: Buscar Usuário por UUID Public ID

* **Método HTTP**: `GET`
* **URL**: `/api/usuarios/{publicId}` (ex: `/api/usuarios/f8a9271d-6055-4588-9a69-d84bce833030`)
* **Autenticação Necessária**: Sim (`Authorization: Bearer <token>`).

#### Resposta do Backend (`200 OK`): Retorna o objeto `UsuarioResponseDTO` individual.

---

## 6. GUIA TÉCNICO DE INTEGRAÇÃO COM O FRONTEND

Para conectar a aplicação Frontend (seja desenvolvida em React, Vue, Angular ou JavaScript Vanilla) com esta API RESTful:

### 1. URL Base

A API executa por padrão no endereço: `http://localhost:8080`

### 2. Padrão de Cabeçalhos (Headers)

Para requisições sem autenticação (Cadastro e Login):

```http
Content-Type: application/json

```

Para requisições protegidas (Listagem, Consultas, Agendamentos):

```http
Content-Type: application/json
Authorization: Bearer <TOKEN_JWT_AQUI>

```

### 3. Persistência de Sessão no Frontend

Ao receber a resposta do `POST /api/auth/login`, o frontend DEVE armazenar os seguintes itens no `localStorage` ou `sessionStorage`:

* `token`: Guardar a string do JWT.
* `usuarioPublicId`: Guardar o UUID para associar requisições futuras do usuário ativo.
* `cargo`: Guardar a role para controle de exibição de menus (ex: se `cargo === 'ADMINISTRADOR'`, exibe menu administrativo).

---

## 7. FLUXO DO USUÁRIO PASSO A PASSO

### Exemplo: Fluxo de Autenticação e Entrada no Dashboard

```
+---------------+     1. Digita email e senha e clica "Entrar"     +-------------------+
| Tela de Login | -----------------------------------------------> | Client Frontend   |
+---------------+                                                  +-------------------+
                                                                             |
                                                            2. POST /api/auth/login
                                                                             v
+---------------+       4. Devolve Token JWT + usuarioPublicId     +-------------------+
| API Backend   | -----------------------------------------------> | Client Frontend   |
+---------------+                                                  +-------------------+
                                                                             |
                                                            3. Salva Token localStorage
                                                            4. Redireciona /dashboard
                                                                             v
+---------------+     5. GET /api/usuarios/{publicId}              +-------------------+
| Tela Dashboard| <----------------------------------------------- | Client Frontend   |
|   (Protegida) |                                                  | Header: Bearer    |
+---------------+                                                  +-------------------+

```

---

## 8. DETALHAMENTO COMPLETO DO PROCESSO DE LOGIN

### Estrutura Completa do Fluxo Executado pelo Backend:

```
[Usuário] -> Formulario Login
   |
   +--> [Frontend] realiza fetch('POST /api/auth/login', body: {login, senha})
           |
           v
        [AuthController.java] (@PostMapping("/login"))
           |
           +--> Chama [AuthService.java] .autenticar(LoginDTO)
                   |
                   +--> [UsuarioRepository] .findByEmailOrCpf(login, login)
                   |       |
                   |       +--> Se não achar -> throw IllegalArgumentException("Usuário ou senha inválidos.")
                   |
                   +--> [BCryptPasswordEncoder] .matches(senhaPura, usuario.getSenha())
                   |       |
                   |       +--> Se false -> throw IllegalArgumentException("Usuário ou senha inválidos.")
                   |
                   +--> [TokenService.java] .gerarToken(usuario)
                   |       |
                   |       +--> Assina JWT com Subject, PublicID, Cargo e Expiração (2h)
                   |
                   +--> Retorna [TokenDTO] com Token + Dados do Usuário
                           |
                           v
        [GlobalExceptionHandler.java] (Captura exceções se houver e formata resposta)
           |
           v
        [Frontend] Recebe Status 200 OK + JSON Payload

```

### Comportamento em Caso de Sucesso (`HTTP 200 OK`):

O Frontend recebe a chave `token`, grava no `localStorage.setItem('token', data.token)` e navega a aplicação para a tela principal/dashboard.

### Comportamento em Caso de Erro (`HTTP 401 Unauthorized`):

O `GlobalExceptionHandler` captura a exceção da regra de negócio e retorna o código `401`. O Frontend deve ler o campo `message` da resposta JSON e exibir um toast/alerta na tela para o usuário (ex: "Usuário ou senha incorretos").

---

## 9. PASSOS SEGUINTES APÓS A AUTENTICAÇÃO

Com o módulo de autenticação e gerenciamento de usuários finalizado e funcional:

```
                                  +-----------------------+
                                  | Login Realizado (200) |
                                  +-----------------------+
                                              |
                                              v
                                  +-----------------------+
                                  | Guardar Token no      |
                                  | localStorage          |
                                  +-----------------------+
                                              |
                                              v
                        +-------------------------------------------+
                        | Selecionar Próximo Módulo para Integração |
                        +-------------------------------------------+
                                              |
            +---------------------------------+---------------------------------+
            |                                 |                                 |
            v                                 v                                 v
+-----------------------+         +-----------------------+         +-----------------------+
| Tela de Agendamento   |         | Tela de Exames        |         | Painel Administrativo |
| (Pacientes)           |         | (Médicos/Técnicos)    |         | (Gestão de Cargos)    |
+-----------------------+         +-----------------------+         +-----------------------+

```

1. **Implementar Interceptor HTTP no Frontend**: Configurar o cliente HTTP (`axios` ou wrapper de `fetch`) para incluir o header `Authorization: Bearer <token>` em todas as chamadas subsequentes automaticamente.
2. **Desenvolver o Filtro de Segurança do Backend (`SecurityFilter.java`)**: Conectar o token enviado pelo frontend com a validação do Spring Security para bloquear requisições não autenticadas no nível do protocolo HTTP.
3. **Módulo de Exames e Consultas**: Construir as interfaces de agendamento consumindo os próximos endpoints de negócios.

---

## 10. GUIA DE IMPLEMENTAÇÃO DAS FUNCIONALIDADES PARA O FRONTEND

### Funcionalidade: Cadastro de Pacientes / Usuários

* **Tela**: `/cadastro` ou `/usuarios/novo`
* **Ação do Usuário**: Preenche o formulário com dados pessoais e endereço completo.
* **Chamada HTTP**: `POST /api/usuarios`
* **Payload Enviado**: Objeto `UsuarioRequestDTO` completo contendo dados do endereço embutidos.
* **Tratamento de Erro**: Se a API retornar `400 Bad Request`, ler o campo de erro referente a CPF inválido ou e-mail duplicado e destacar o input correspondente no formulário.

### Funcionalidade: Perfil do Usuário

* **Tela**: `/perfil`
* **Ação do Usuário**: Acessa a tela para visualizar seus dados.
* **Chamada HTTP**: `GET /api/usuarios/{usuarioPublicId}` (utilizando o `usuarioPublicId` armazenado no `localStorage` após o login).
* **Tratamento da Resposta**: Preencher as labels e campos da tela com os dados retornados no `UsuarioResponseDTO`.

---

## 11. EXEMPLOS PRÁTICOS DE CÓDIGO DE COMUNICAÇÃO HTTP (JAVASCRIPT / FETCH)

### 1. Função de Login (`POST /api/auth/login`)

```javascript
async function efetuarLogin(login, senha) {
  try {
    const response = await fetch('http://localhost:8080/api/auth/login', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({ login, senha })
    });

    if (!response.ok) {
      const errorData = await response.json();
      throw new Error(errorData.message || 'Falha ao realizar login.');
    }

    const data = await response.json();
    
    // Armazena credenciais e dados da sessão
    localStorage.setItem('token', data.token);
    localStorage.setItem('usuarioPublicId', data.usuarioPublicId);
    localStorage.setItem('cargo', data.cargo);

    console.log('Login efetuado com sucesso! Token armazenado.');
    return data;
  } catch (error) {
    console.error('Erro no login:', error.message);
    alert(error.message);
  }
}

```

### 2. Requisição Autenticada (`GET /api/usuarios`)

```javascript
async function buscarUsuarios() {
  const token = localStorage.getItem('token');

  try {
    const response = await fetch('http://localhost:8080/api/usuarios', {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
      }
    });

    if (response.status === 401) {
      alert('Sessão expirada. Faça login novamente.');
      window.location.href = '/login.html';
      return;
    }

    const usuarios = await response.json();
    console.log('Lista de Usuários:', usuarios);
    return usuarios;
  } catch (error) {
    console.error('Erro ao buscar usuários:', error);
  }
}

```

---

## 12. MAPEAMENTO DETALHADO DOS JSONs (REQUEST / RESPONSE)

### Payload de Cadastro (`UsuarioRequestDTO`)

#### Request:

```json
{
  "nome": "String (Obrigatório)",
  "cpf": "String com 11 dígitos numéricos válidos (Obrigatório)",
  "email": "String com formato válido de e-mail (Obrigatório)",
  "senha": "String com no mínimo 6 caracteres (Obrigatório)",
  "telefone": "String contendo DDD + número (Obrigatório)",
  "dataNascimento": "String no formato YYYY-MM-DD (Obrigatório)",
  "cargoId": "Long contendo o ID do cargo existente no banco (Obrigatório)",
  "endereco": {
    "cep": "String com 8 dígitos (Obrigatório)",
    "logradouro": "String (Obrigatório)",
    "numero": "String (Obrigatório)",
    "complemento": "String (Opcional)",
    "bairro": "String (Obrigatório)",
    "cidade": "String (Obrigatório)",
    "estado": "String com a UF em 2 letras (Obrigatório)"
  }
}

```

---

## 13. TRATAMENTO E FORMATO PADRÃO DE ERROS DA API

A API utiliza o manipulador global `@RestControllerAdvice` (`GlobalExceptionHandler.java`) para padronizar as respostas de erro HTTP.

### Formato Padrão de Erro Retornado pela API:

```json
{
  "timestamp": "2026-09-27T11:54:35.123456",
  "status": 401,
  "error": "Unauthorized",
  "message": "Usuário ou senha inválidos."
}

```

### Tabela de Status HTTP Suportados e Ações Recomendadas no Frontend:

| Status HTTP | Causa no Backend | Ação Recomendada no Frontend |
| --- | --- | --- |
| **`400 Bad Request`** | Erro de validação de campo (`@Valid`), CPF inválido ou e-mail já cadastrado. | Exibir caixa de erro destacando o campo incorreto digitado pelo usuário. |
| **`401 Unauthorized`** | Credenciais inválidas no login ou Token JWT expirado/ausente. | Redirecionar o usuário para a tela de login e limpar o `localStorage`. |
| **`403 Forbidden`** | O usuário autenticado não possui o Cargo/Role necessário para acessar a rota. | Exibir mensagem de acesso negado ("Você não tem permissão para acessar este recurso"). |
| **`404 Not Found`** | Recurso/Usuário não encontrado para o `publicId` fornecido. | Exibir página de erro 404 ou aviso de registro inexistente. |
| **`500 Internal Error`** | Exceções não capturadas no servidor. | Informar ao usuário para tentar novamente mais tarde e logar a falha no console. |

---

## 14. DEPENDÊNCIAS ENTRE REQUISIÇÕES E DEPENDENCY GRAPH

O Frontend deve respeitar a ordem cronológica de dependência entre chamadas da API:

```
[1. GET /api/cargos ou id estático]
         |
         v
[2. POST /api/usuarios] -------------> Retorna: publicId
                                              |
                                              v
                                   [3. POST /api/auth/login] ----> Retorna: token JWT
                                                                          |
                                                                          v
                                                               [4. Chamadas Autenticadas com Token]

```

---

## 15. MANIPULAÇÃO DE RELACIONAMENTOS E UUIDs NO FRONTEND

O Frontend **nunca** deve lidar com as chaves primárias numéricas autoincrementáveis (`id: 1, 2, 3`) das entidades principais (`Usuario`).

* Para identificar e navegar entre usuários, utilize exclusivamente a propriedade **`publicId`** (formato String/UUID, ex: `f8a9271d-6055-4588-9a69-d84bce833030`).
* Os IDs numéricos são aceitos apenas em referências estáticas de tabelas de domínio simples, como a seleção de cargos (`cargoId: 1`).

---

## 16. MATRIZ DE STATUS DE IMPLEMENTAÇÃO DO PROJETO

### Backend Implementado:

* [x] Modelagem das Tabelas `usuarios`, `enderecos`, `cargos`.
* [x] Entidades JPA com `@PrePersist` para UUIDs e relacionamentos.
* [x] DTOs com Java Records e Validações (`Jakarta Validation`).
* [x] Hashing de Senhas via `BCryptPasswordEncoder`.
* [x] Emissão de Tokens JWT via `TokenService` (Auth0).
* [x] Endpoint de Autenticação (`POST /api/auth/login`).
* [x] Endpoints CRUD de Usuários (`POST /api/usuarios`, `GET /api/usuarios`, `GET /api/usuarios/{publicId}`).
* [x] Tratamento Global de Exceções (`GlobalExceptionHandler.java`).

### Backend Parcialmente Implementado / Em Desenvolvimento:

* [ ] Filtro de Segurança Spring Security (`SecurityFilter.java`) para validação automática do Header `Authorization` nas rotas protegidas.

### Funcionalidades do Backend Ainda Não Existentes (Módulos Futuros):

* [ ] Endpoints e Tabelas de **Exames**.
* [ ] Endpoints e Tabelas de **Agendamentos / Consultas**.
* [ ] Processamento Assíncrono de Solicitações.

---

## 17. TABELA DE INTEGRAÇÃO FRONTEND × BACKEND

| Funcionalidade | Tela Frontend | Endpoint API | Método | Body Enviado (Request) | Resposta Retornada | Autenticação |
| --- | --- | --- | --- | --- | --- | --- |
| **Login no Sistema** | `/login` | `/api/auth/login` | `POST` | `LoginDTO` (`login`, `senha`) | `TokenDTO` (`token`, `usuarioPublicId`, `cargo`...) | Não |
| **Cadastrar Paciente** | `/cadastro` | `/api/usuarios` | `POST` | `UsuarioRequestDTO` (com endereço embutido) | `UsuarioResponseDTO` | Não / Opcional |
| **Listar Usuários** | `/usuarios` | `/api/usuarios` | `GET` | *Nenhum* | `Array<UsuarioResponseDTO>` | **Sim (Bearer)** |
| **Buscar Perfil** | `/perfil` | `/api/usuarios/{publicId}` | `GET` | *Nenhum* | `UsuarioResponseDTO` | **Sim (Bearer)** |

---

## 18. ESTRUTURA RECOMENDADA PARA O PROJETO FRONTEND

Para manter a separação de conceitos e evitar chamadas de rede diretamente dentro dos componentes visuais, recomenda-se a seguinte organização de pastas:

```
src/
├── assets/             # Imagens, ícones, estilos globais
├── components/         # Componentes visuais reutilizáveis (Botões, Inputs, Modais)
├── services/           # Camada de comunicação com a API
│   ├── api.js          # Configuração base do fetch/axios (URL base, Interceptors)
│   ├── authService.js  # Funções de Login, Logout e verificação de Token
│   └── userService.js  # Funções CRUD de Usuários
├── pages/              # Páginas da aplicação
│   ├── Login/          # Tela de Login
│   ├── Register/       # Tela de Cadastro
│   └── Dashboard/      # Painel Principal
├── utils/              # Formatadores (Formatação de CPF, CEP, Datas)
└── routes/             # Gerenciamento de rotas e Proteção de Acesso (Guards)

```

---

## 19. PONTOS DE ATENÇÃO E OPORTUNIDADES DE MELHORIA NO PROJETO

1. **Ativação do `SecurityFilter.java**`:
* **Problema**: O `TokenService` já gera e valida os tokens JWT perfeitamente, mas falta implementar a classe do filtro do Spring Security que intercepta as requisições HTTP e valida o cabeçalho `Authorization: Bearer <token>` em cada rota protegida.
* **Impacto**: Atualmente, requisições no endpoint `GET /api/usuarios` passam direto mesmo sem o Token no header.
* **Solução**: Implementar o `SecurityFilter.java` estendendo `OncePerRequestFilter` e registrá-lo na corrente de filtros do Spring Security.


2. **Configuração de CORS (Cross-Origin Resource Sharing)**:
* **Problema**: Ao tentar fazer chamadas da API via chamadas `fetch()` a partir do navegador em portas diferentes (ex: frontend rodando em `http://localhost:3000` ou `5173` e backend em `8080`), o navegador bloqueará as requisições por falta dos cabeçalhos CORS.
* **Solução**: Adicionar a anotação `@CrossOrigin(origins = "*")` nos Controllers ou criar uma classe global de configuração do WebMvc (`WebMvcConfigurer`) liberando as origens de desenvolvimento.



---

## 20. GUIA PRÁTICO PARA CONTINUAR O DESENVOLVIMENTO DO FRONTEND

### Passo 1: Execução do Backend

1. Certifique-se de que o banco PostgreSQL está rodando localmente na porta `5432`.
2. Certifique-se de que a propriedade `spring.jpa.hibernate.ddl-auto` no `application.properties` esteja configurada como `update` (ou `create` caso deseje resetar o banco).
3. Execute a classe principal da aplicação Spring Boot na sua IDE ou pelo terminal:
```bash
./mvnw spring-boot:run

```



### Passo 2: Testando a Conectividade da API

Abra o navegador ou o cliente HTTP (Insomnia/Postman) e faça uma requisição `GET` para:
`http://localhost:8080/api/usuarios`
A resposta deve ser um array JSON `[]` com código HTTP `200 OK`.

### Passo 3: Fluxo Completo de Testes no Frontend

1. **Submeter Cadastro**: Envie um `POST /api/usuarios` contendo um payload válido.
2. **Submeter Login**: Execute o `POST /api/auth/login` com os mesmos e-mail/CPF e senha cadastrados.
3. **Receber e Armazenar o Token**: Garanta que o token JWT retornado foi gravado no `localStorage`.
4. **Navegação**: Redirecione a aplicação para a área autenticada e utilize o `token` armazenado nos headers para realizar as requisições subsequentes.

---

## 21. RESUMO FINAL DO PROJETO

* **Estado Atual do Backend**: Robustamente estruturado no padrão MVC em camadas. Possui criptografia de senhas com **BCrypt**, geração de tokens **JWT**, gerenciamento de dados de **Usuários**, **Endereços** e **Cargos**, com validações sintáticas e de regras de negócio em dia.
* **Estado Atual do Frontend**: Autenticação via formulário integrada e pronta para expansão dos painéis internos.
* **Próximos Passos Imediatos**:
1. Concluir o `SecurityFilter.java` no backend para fechar a camada de proteção HTTP.
2. Adicionar o cabeçalho de CORS `@CrossOrigin` nos Controllers do Spring Boot.
3. Expandir o frontend construindo os painéis de agendamentos e listagens consumindo os DTOs documentados neste relatório.

Aqui está a documentação técnica detalhada e completa de todas as funcionalidades, regras de negócio, estrutura de dados e fluxos construídos após as etapas iniciais de autenticação (`/api/auth/login` e `/api/auth/registrar`).

---

## 1. Gestão de Pacientes e Dependentes

O modelo de domínio separa a conta de acesso (`Usuario`) das entidades físicas atendidas no hospital (`Paciente`). No momento do cadastro do usuário, o sistema cria automaticamente o registro do `Paciente` com parentesco `TITULAR`.

### Estrutura de Relacionamento

* **Titular:** Vinculado diretamente ao `Usuario` autenticado (`parentesco = "TITULAR"`).
* **Dependente:** Registrado sob a responsabilidade do usuário (`parentesco = "FILHO"`, `"CONJUGE"`, `"MAE"`, `"PAI"`, etc.), mantendo a chave estrangeira `usuario_responsavel_id`.

### Endpoints

* **`POST /api/pacientes/dependentes`**
* **Objetivo:** Cadastrar um dependente associado ao usuário logado.
* **Payload (`DependenteRequestDTO`):** `nome`, `cpf`, `dataNascimento`, `parentesco`.
* **Regras de Negócio:**
* O e-mail do solicitante é extraído do token JWT.
* Valida a duplicidade de CPF no banco de dados.
* Gera um UUID público (`publicId`) exclusivo para o dependente.


* **Resposta:** `201 Created` contendo os dados do dependente criado (`PacienteResponseDTO`).


* **`GET /api/pacientes/meus-pacientes`**
* **Objetivo:** Listar todos os pacientes associados à conta (Titular + Dependentes).
* **Regras de Negócio:** Busca no banco por todos os registros onde `usuario_responsavel_id` é igual ao ID do usuário autenticado.
* **Resposta:** `200 OK` com a lista de DTOs dos pacientes.



---

## 2. Módulo de Agendamento de Exames (Visão do Paciente)

O agendamento funciona via modelo de **intenção/solicitação**: o paciente não agenda diretamente em um horário fixo, mas sugere **3 opções de datas/horários de preferência** para avaliação da recepção.

### Arquitetura de 1 Exame por Agendamento

Para evitar gargalos de logística (salas diferentes, preparos específicos e tempos de execução distintos), cada solicitação é estritamente individual:


$$\text{1 Solicitação} = \text{1 Paciente} + \text{1 Exame} + \text{3 Sugestões de Data}$$

### Endpoints

* **`POST /api/agendamentos`**
* **Objetivo:** Criar uma solicitação de agendamento.
* **Payload (`AgendamentoRequestDTO`):** `pacientePublicId` (opcional), `examePublicId`, `dataOpcao1`, `dataOpcao2`, `dataOpcao3`, `observacao`.
* **Regras de Negócio:**
* **Identificação do Paciente:** Se `pacientePublicId` for informado, o sistema busca o dependente correspondente. Se for `null`, o sistema assume automaticamente o paciente `TITULAR` do usuário logado.
* **Trava Anti-Duplicidade / Regra de Pendência Ativa:** O sistema executa a checagem no repositório via `existsByPacienteIdAndExameIdAndStatus(pacienteId, exameId, "PENDENTE")`. Se o paciente já possuir uma solicitação em status `PENDENTE` para o mesmo exame, a operação é bloqueada disparando `BusinessException` (`400 Bad Request`).
* **Gravação:** O agendamento é salvo com status inicial `PENDENTE`.




* **`GET /api/agendamentos/meus-agendamentos`**
* **Objetivo:** Consultar o histórico de solicitações do usuário logado.
* **Resposta:** Lista de DTOs contendo status, datas sugeridas, data confirmada (se houver) e observações.


* **`GET /api/agendamentos/{publicId}`**
* **Objetivo:** Buscar os detalhes de um agendamento específico.
* **Regras de Negócio:** Filtra por `publicId` e garante que o agendamento pertence ao `solicitantePublicId` do usuário autenticado (segurança no nível de linha).


* **`PATCH /api/agendamentos/{publicId}/cancelar`**
* **Objetivo:** Permitir que o paciente cancele uma solicitação.
* **Regras de Negócio:**
* Não permite cancelar agendamentos já em status `CANCELADO` ou `REALIZADO`.
* Altera o status para `CANCELADO`, liberando o paciente para realizar novas solicitações do mesmo exame se desejar.





---

## 3. Módulo de Atendimento e Recepção (Visão do Admin)

O módulo administrativo gerencia a fila de triagem das solicitações e a alocação física da agenda do hospital.

### Regra de Ouro da Agenda

A reserva de horário **não é feita no status `PENDENTE**` (evitando bloqueio triplo indevido de vagas). O bloqueio de grade e a verificação de choque de horário ocorrem estritamente no momento da **confirmação pelo atendente**.

### Endpoints

* **`GET /api/admin/agendamentos/pendentes`**
* **Objetivo:** Retornar a fila de trabalho da recepção.
* **Regras de Negócio:** Filtra todos os registros com status `PENDENTE`, ordenados de forma ascendente pela `dataCriacao` (ordem de chegada).


* **`PATCH /api/admin/agendamentos/{publicId}/confirmar`**
* **Objetivo:** Aprovar e efetivar a reserva do agendamento.
* **Payload (`ConfirmarAgendamentoDTO`):** `dataConfirmada`.
* **Regras de Negócio:**
1. **Validação de Status:** Garante que o agendamento está `PENDENTE`.
2. **Validação de Opções do Paciente:** O valor de `dataConfirmada` deve ser estritamente igual a `dataOpcao1`, `dataOpcao2` ou `dataOpcao3`.
3. **Trava de Conflito de Horário (Choque de Agenda):** Executa a consulta no repositório:

$$\text{Conflito} = \text{Exame ID} \land \text{Status} = \text{'CONFIRMADO'} \land \text{Data Confirmada} = \text{Data Escolhida}$$



Se retornar `true`, a confirmação é bloqueada com mensagem de erro de horário ocupado.
4. **Efetivação:** Define a `dataConfirmada`, altera o status para `CONFIRMADO` e persiste na base.




* **`PATCH /api/admin/agendamentos/{publicId}/recusar`**
* **Objetivo:** Indeferir uma solicitação de agendamento.
* **Payload (`RecusarAgendamentoDTO`):** `motivoRecusa`.
* **Regras de Negócio:** Altera o status para `RECUSADO` e anexa a justificativa no campo de observação.



---

## 4. Tabela Consolidada do Módulo de Dados e Status

### Ciclo de Vida do Status do Agendamento

| Status | Origem | Descrição |
| --- | --- | --- |
| `PENDENTE` | Sistema (`POST /agendamentos`) | Solicitação criada pelo paciente aguardando análise da recepção. |
| `CONFIRMADO` | Atendente (`PATCH /confirmar`) | Vaga garantida na agenda com `dataConfirmada` preenchida. |
| `RECUSADO` | Atendente (`PATCH /recusar`) | Solicitação negada pela recepção com justificativa registrada. |
| `CANCELADO` | Paciente (`PATCH /cancelar`) | Solicitação anulada pelo próprio usuário. |
| `REALIZADO` | Sistema / Recepção | Exame concluído (status final). |

---

## 5. Mapeamento de Exceções e Respostas da API

* **`BusinessException` (`400 Bad Request`):** Disparada em violações de regra de negócio (ex: tentativa de criar agendamento pendente duplicado, confirmação com data diferente das 3 opções, ou choque de horário na agenda).
* **`ResourceNotFoundException` (`404 Not Found`):** Disparada quando UUIDs de usuários, pacientes, exames ou agendamentos não são localizados na base.
* **`MethodArgumentNotValidException` (`400 Bad Request`):** Disparada automaticamente pelo Spring Validation em payloads com campos nulos ou em branco.
Aqui está a especificação técnica focada no **Front-end (React / Next.js / Vue)**. Ela cobre os estados Globais, telas, fluxos de navegação, formulários e integração direta com a API que foi construída.

---

## 1. Mapeamento de Telas por Perfil de Usuário

### Perfil: Paciente / Cliente

```
[Login / Cadastro] ──> [Dashboard do Paciente]
                             │
                             ├──> [Meus Pacientes / Dependentes] ──> (Modal: Novo Dependente)
                             │
                             ├──> [Novo Agendamento (Wizard)]
                             │        ├── Passo 1: Selecionar Paciente (Titular ou Dependente)
                             │        ├── Passo 2: Escolher Exame
                             │        └── Passo 3: Escolher 3 Opções de Data/Hora
                             │
                             └──> [Meus Agendamentos (Listagem)]
                                      └──> (Modal / Drawer: Detalhes do Agendamento + Botão Cancelar)

```

---

### Perfil: Atendente / Recepção (Admin)

```
[Login Admin] ──> [Painel de Recepção / Fila de Triagem]
                        │
                        ├──> [Aba: Solicitações Pendentes] ──> (Drawer de Decisão)
                        │                                          ├── Botão: Confirmar (Select de 1 das 3 datas)
                        │                                          └── Botão: Recusar (Input: Motivo)
                        │
                        └──> [Aba: Agenda Confirmada / Grade de Exames]

```

---

## 2. Estrutura de Estado e Dados no Front-end

### DTOs Mapeados para Interfaces TypeScript (`src/types/index.ts`)

```typescript
export type StatusAgendamento = 'PENDENTE' | 'CONFIRMADO' | 'RECUSADO' | 'CANCELADO' | 'REALIZADO';

export interface Paciente {
  publicId: string;
  nome: string;
  cpf: string;
  dataNascimento: string;
  parentesco: 'TITULAR' | 'FILHO' | 'CONJUGE' | 'MAE' | 'PAI' | 'OUTRO';
}

export interface Agendamento {
  publicId: string;
  pacientePublicId: string;
  solicitantePublicId: string;
  examePublicId: string;
  status: StatusAgendamento;
  dataOpcao1: string; // ISO 8601 string
  dataOpcao2: string;
  dataOpcao3: string;
  dataConfirmada?: string;
  observacao?: string;
  dataCriacao: string;
}

export interface AgendamentoRequestDTO {
  pacientePublicId?: string; // Se nulo, backend assume o Titular
  examePublicId: string;
  dataOpcao1: string;
  dataOpcao2: string;
  dataOpcao3: string;
  observacao?: string;
}

```

---

## 3. Especificação de Componentes e Comportamento Visual

### Tela 1: Form de Novo Agendamento (Visão Paciente)

**Comportamento do Formulário:**

1. **Seleção do Paciente:** Um `<select>` ou radio card alimentado pelo endpoint `GET /api/pacientes/meus-pacientes`. Mostra "Para mim (Titular)" ou o nome do dependente (ex: "Lucas (Filho)").
2. **Seleção de Data e Hora:** 3 inputs de `<input type="datetime-local" />`.
* **Validação de Front-end:** Impede a escolha de datas passadas (`min = new Date()`) e valida se a `dataOpcao2` e `dataOpcao3` são diferentes entre si.


3. **Tratamento do Botão Enviar (Anti-Spam de Cliques):**
* Desabilita o botão imediatamente no clique (`disabled={loading}`) e exibe um spinner.
* Se a API retornar `400 Bad Request` com a mensagem *"Já existe uma solicitação pendente..."*, exibe um **Alert / Toast de Aviso (Amarelo)**: *"Você já possui um pedido em análise para este exame. Cancele o anterior para enviar um novo."* com botão rápido redirecionando para a aba "Meus Agendamentos".



---

### Tela 2: Fila de Triagem da Recepção (Visão Admin)

**Layout Recomendado:** Tabela ou lista de cards filtráveis ordenados por ordem de chegada (`GET /api/admin/agendamentos/pendentes`).

| Paciente / Solicitante | Exame | Opções Sugeridas pelo Paciente | Ações |
| --- | --- | --- | --- |
| **João Silva** *(Dependente)*<br>

<br>`Resp: Maria Silva` | Hemograma Completo | **Opção 1:** 20/10/2026 08:00<br>

<br>**Opção 2:** 21/10/2026 09:00<br>

<br>**Opção 3:** 22/10/2026 10:00 | `<Button variant="success">` Analisar / Aprovar<br>

<br>`<Button variant="danger">` Recusar |

**Modal de Confirmação pelo Atendente:**
Ao clicar em "Analisar", abre um Modal com os detalhes do pedido:

1. O atendente vê os 3 radio buttons correspondentes às 3 datas exatas enviadas pelo paciente.
2. O atendente seleciona UMA das datas e clica em **"Efetivar Agendamento"**.
3. **Tratamento de Conflito no Front-end:**
* Se a API retornar `400 Bad Request` (*"Este horário já está ocupado por outro agendamento confirmado"*), o Modal **não fecha** e exibe um toast de Erro: *"Horário ocupado na grade do exame. Selecione outra das opções enviadas pelo paciente."*



---

## 4. Gestão de Rotas e Segurança de Acesso no Front-end

Configuração de navegação baseada nas *roles* extraídas do Token JWT (`ROLE_USER`, `ROLE_ADMIN`, `ROLE_RECEPCAO`):

```typescript
// Exemplo de Proteção de Rota em React Router
const PrivateRoute = ({ allowedRoles }: { allowedRoles: string[] }) => {
  const { user, token } = useAuth();

  if (!token) return <Navigate to="/login" replace />;
  if (!allowedRoles.includes(user.role)) return <Navigate to="/unauthorized" replace />;

  return <Outlet />;
};

```

| Rota Front-end | Roles Permitidas | Componente / Tela |
| --- | --- | --- |
| `/agendamentos/novo` | `ROLE_USER` | Form de Solicitação de Exames |
| `/meus-agendamentos` | `ROLE_USER` | Histórico e Cancelamento pelo Paciente |
| `/dependentes` | `ROLE_USER` | Cadastro e Listagem de Dependentes |
| `/admin/pendentes` | `ROLE_ADMIN`, `ROLE_RECEPCAO` | Fila de Triagem e Confirmação de Grade |

---

## 5. Estados Globais e Interceptadores (Axios / Fetch)

Para o front-end funcionar sem falhas com o Spring Boot:

1. **Request Interceptor:** Anexa o token `Bearer <token>` no header `Authorization` de toda requisição enviada.
2. **Response Interceptor (Erro 401/403):** Se o token expirar, limpa o `localStorage` e redireciona o usuário automaticamente para a tela de `/login`.
3. **Tratamento de Mensagens de Erro Padrão:** O front-end deve ler a chave `message` enviada pelo `RestExceptionHandler` da API Spring para alimentar os Toasts/Alerts visuais da tela.

Aqui está a **especificação técnica completa de integração Front-end <-> Back-end** com o fluxo de autenticação, o ciclo de vida das requisições, os contratos das rotas e o tratamento de estados.

---

## 1. Arquitetura de Comunicação e Segurança

### Fluxo do Token JWT

1. O usuário se autentica na rota `/api/auth/login`.
2. O backend retorna o token JWT e o perfil (`ROLE_USER` ou `ROLE_ADMIN`/`ROLE_RECEPCAO`).
3. O front-end armazena o token (em `localStorage`, `sessionStorage` ou `Cookies`) e o insere no header de todas as chamadas protegidas:
`Authorization: Bearer <TOKEN_JWT>`

### Configuração do Cliente HTTP (Exemplo com Axios)

```javascript
import axios from 'axios';

const api = axios.create({
  baseURL: 'http://localhost:8080/api',
});

// Interceptor de Requisição: Injeta o Token automaticamente
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Interceptor de Resposta: Trata sessão expirada e erros de validação
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

export default api;

```

---

## 2. Guia Completo das Rotas da API

### Módulo 1: Autenticação (`/api/auth`)

| Método | Endpoint | Descrição | Envio no Body | Resposta de Sucesso |
| --- | --- | --- | --- | --- |
| `POST` | `/api/auth/login` | Login do usuário | `{ email, senha }` | `200 OK` → `{ token, role }` |
| `POST` | `/api/auth/registrar` | Novo cadastro | `{ nome, email, senha, cpf, dataNascimento }` | `201 Created` → `{ id, publicId, email }` |

> **Nota do Front-end:** Ao registrar um novo usuário, o Spring Boot gera automaticamente a conta de acesso (`Usuario`) e a entidade do paciente principal com `parentesco = "TITULAR"`.

---

### Módulo 2: Pacientes e Dependentes (`/api/pacientes`)

#### 1. Cadastrar Dependente

* **Método/Rota:** `POST /api/pacientes/dependentes`
* **Headers:** `Authorization: Bearer <TOKEN>`
* **Body (JSON):**
```json
{
  "nome": "Pedro Silva",
  "cpf": "123.456.789-00",
  "dataNascimento": "2015-05-20",
  "parentesco": "FILHO"
}

```


* **Comportamento no Front:** Abre num modal ou formulário secundário. Após a resposta `201 Created`, atualiza a lista local de dependentes.

#### 2. Listar Meus Pacientes (Titular + Dependentes)

* **Método/Rota:** `GET /api/pacientes/meus-pacientes`
* **Headers:** `Authorization: Bearer <TOKEN>`
* **Resposta de Sucesso (`200 OK`):**
```json
[
  {
    "publicId": "a1b2c3d4-0000-0000-0000-000000000001",
    "nome": "João Silva (Você)",
    "cpf": "000.000.000-00",
    "dataNascimento": "1990-01-01",
    "parentesco": "TITULAR"
  },
  {
    "publicId": "b2c3d4e5-1111-1111-1111-111111111112",
    "nome": "Pedro Silva",
    "cpf": "123.456.789-00",
    "dataNascimento": "2015-05-20",
    "parentesco": "FILHO"
  }
]

```


* **Comportamento no Front:** Alimenta o `<select>` ou os cards de escolha de paciente na tela de criação de agendamento.

---

### Módulo 3: Agendamentos — Visão do Paciente (`/api/agendamentos`)

#### 1. Solagendar / Criar Solicitação

* **Método/Rota:** `POST /api/agendamentos`
* **Body (JSON):**
```json
{
  "pacientePublicId": "b2c3d4e5-1111-1111-1111-111111111112", // Opcional (se null, backend assume o Titular)
  "examePublicId": "f9e8d7c6-5555-5555-5555-555555555555",
  "dataOpcao1": "2026-10-25T08:00:00Z",
  "dataOpcao2": "2026-10-26T09:30:00Z",
  "dataOpcao3": "2026-10-27T14:00:00Z",
  "observacao": "Jejum de 8h necessário."
}

```


* **Respostas HTTP Possíveis:**
* `201 Created`: Solicitado com sucesso (status entra como `PENDENTE`).
* `400 Bad Request`: Disparado pela trava anti-duplicidade. Exibe a mensagem da API em um Alert: *"O paciente já possui uma solicitação pendente para este exame. Aguarde a análise da recepção..."*



#### 2. Listar Meus Agendamentos

* **Método/Rota:** `GET /api/agendamentos/meus-agendamentos`
* **Resposta de Sucesso (`200 OK`):**
```json
[
  {
    "publicId": "c3d4e5f6-2222-2222-2222-222222222223",
    "pacientePublicId": "b2c3d4e5-1111-1111-1111-111111111112",
    "solicitantePublicId": "a1b2c3d4-0000-0000-0000-000000000001",
    "examePublicId": "f9e8d7c6-5555-5555-5555-555555555555",
    "status": "PENDENTE",
    "dataOpcao1": "2026-10-25T08:00:00Z",
    "dataOpcao2": "2026-10-26T09:30:00Z",
    "dataOpcao3": "2026-10-27T14:00:00Z",
    "dataConfirmada": null,
    "observacao": "Jejum de 8h.",
    "dataCriacao": "2026-10-07T22:00:00Z"
  }
]

```



#### 3. Buscar Detalhes por UUID

* **Método/Rota:** `GET /api/agendamentos/{publicId}`
* **Comportamento no Front:** Usado para abrir a tela/modal com os detalhes e opções de data de um agendamento específico.

#### 4. Cancelar Agendamento pelo Paciente

* **Método/Rota:** `PATCH /api/agendamentos/{publicId}/cancelar`
* **Body:** Vazio
* **Comportamento no Front:** Altera o status local para `CANCELADO` e habilita novamente o botão para nova solicitação daquele exame.

---

### Módulo 4: Recepção e Admin (`/api/admin/agendamentos`)

*Exige Token JWT com perfil `ROLE_ADMIN` ou `ROLE_RECEPCAO`.*

#### 1. Fila de Solicitações Pendentes

* **Método/Rota:** `GET /api/admin/agendamentos/pendentes`
* **Comportamento no Front:** Alimenta a tabela principal da recepção. Exibe a ordem de chegada das solicitações em status `PENDENTE`.

#### 2. Confirmar Agendamento (Reserva da Grade)

* **Método/Rota:** `PATCH /api/admin/agendamentos/{publicId}/confirmar`
* **Body (JSON):**
```json
{
  "dataConfirmada": "2026-10-25T08:00:00Z"
}

```


* **Regra do Front:** O componente de confirmação exibe um `<select>` ou 3 botões contendo **exclusivamente as 3 opções** que vieram no objeto do agendamento (`dataOpcao1`, `dataOpcao2` e `dataOpcao3`). O atendente escolhe uma delas e envia.
* **Tratamento de Erro (`400 Bad Request`):** Se a API retornar erro de choque de agenda (*"Este horário já está ocupado..."*), exibe o aviso em vermelho no modal sem fechá-lo, para que o atendente selecione outra das 3 opções.

#### 3. Recusar Agendamento

* **Método/Rota:** `PATCH /api/admin/agendamentos/{publicId}/recusar`
* **Body (JSON):**
```json
{
  "motivoRecusa": "Setor de tomografia em manutenção no período solicitado."
}

```



---

## 3. Matriz de Estados e Renderização de UI

O front-end deve reagir ao campo `status` retornado nos objetos de agendamento conforme a tabela:

| Status vindo da API | Cor do Badge na UI | Ações Permitidas para o Paciente | Ações Permitidas para a Recepção |
| --- | --- | --- | --- |
| **`PENDENTE`** | Amarelo / Warning | Botão "Cancelar Solicitação" | Botões "Aprovar" e "Recusar" |
| **`CONFIRMADO`** | Verde / Success | Exibe a data final em destaque | Exibe detalhes do agendamento fixado |
| **`RECUSADO`** | Vermelho / Danger | Exibe o motivo da recusa | Apenas leitura no histórico |
| **`CANCELADO`** | Cinza / Secondary | Botão "Solicitar Novamente" | Apenas leitura no histórico |

---

## 4. Fluxo Completo de Integração (Passo a Passo)

1. **Paciente faz Login:** Guarda o Token e redireciona para `/agendamentos`.
2. **Abre Form de Agendamento:**
* Executa `GET /api/pacientes/meus-pacientes` para preencher o select de paciente.
* Executa `GET /api/exames` (se houver rota pública/autenticada) para preencher a lista de exames.


3. **Envia o Form:** `POST /api/agendamentos` com as 3 opções.
4. **Atendente entra no Painel:**
* Redirecionado para `/admin/pendentes`.
* Executa `GET /api/admin/agendamentos/pendentes`.
* Clica em "Confirmar" em um item, escolhe a `dataOpcao1` e dispara `PATCH /api/admin/agendamentos/{id}/confirmar`.


5. **Atualização em Tempo Real / Re-fetch:**
* Ao fechar o modal, chama novamente o `GET` das rotas para manter as tabelas atualizadas.