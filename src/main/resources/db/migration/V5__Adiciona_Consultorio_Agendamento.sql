-- Consultório onde o exame será realizado (informado pela recepção ao confirmar o agendamento)
ALTER TABLE agendamentos ADD COLUMN consultorio VARCHAR(100);
