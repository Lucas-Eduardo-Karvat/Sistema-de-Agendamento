-- O SecurityConfig já protege /api/admin/** com a role ADMIN; este é o cargo correspondente.
INSERT INTO cargos (nome) VALUES ('ADMIN') ON CONFLICT (nome) DO NOTHING;
