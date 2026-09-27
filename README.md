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