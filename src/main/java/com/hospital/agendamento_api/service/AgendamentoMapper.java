package com.hospital.agendamento_api.service;

import com.hospital.agendamento_api.dto.AgendamentoResponseDTO;
import com.hospital.agendamento_api.entity.Agendamento;
import com.hospital.agendamento_api.entity.Paciente;
import com.hospital.agendamento_api.entity.Usuario;

/** Converte Agendamento em DTO. Deve ser chamado dentro de uma transação (relações LAZY). */
final class AgendamentoMapper {

    private AgendamentoMapper() {
    }

    static AgendamentoResponseDTO toDTO(Agendamento a) {
        Paciente paciente = a.getPaciente();
        Usuario responsavel = paciente.getUsuarioResponsavel();

        // Titular herda os dados do usuário quando o registro do paciente não os repete.
        String nome = paciente.getNome() != null ? paciente.getNome() : responsavel.getNome();
        String cpf = paciente.getCpf() != null ? paciente.getCpf() : responsavel.getCpf();

        return new AgendamentoResponseDTO(
                a.getPublicId(),
                paciente.getPublicId(),
                a.getSolicitante().getPublicId(),
                a.getExame().getPublicId(),
                a.getStatus(),
                a.getDataOpcao1(),
                a.getDataOpcao2(),
                a.getDataOpcao3(),
                a.getDataConfirmada(),
                a.getObservacao(),
                a.getDataCriacao(),
                nome,
                a.getExame().getNome(),
                a.getConsultorio(),
                cpf,
                responsavel.getTelefone(),
                responsavel.getEmail(),
                a.getAlteradoEm());
    }
}
