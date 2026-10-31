package com.hospital.agendamento_api.service;

import com.hospital.agendamento_api.dto.EnderecoResponseDTO;
import com.hospital.agendamento_api.dto.PacienteRecepcaoResponseDTO;
import com.hospital.agendamento_api.dto.PacienteResponseDTO;
import com.hospital.agendamento_api.dto.UsuarioCadastroRequestDTO;
import com.hospital.agendamento_api.dto.UsuarioResponseDTO;
import com.hospital.agendamento_api.entity.Agendamento;
import com.hospital.agendamento_api.entity.Endereco;
import com.hospital.agendamento_api.entity.Paciente;
import com.hospital.agendamento_api.entity.Usuario;
import com.hospital.agendamento_api.exception.ResourceNotFoundException;
import com.hospital.agendamento_api.repository.AgendamentoRepository;
import com.hospital.agendamento_api.repository.PacienteRepository;
import com.hospital.agendamento_api.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class RecepcaoPacienteService {

    private final UsuarioService usuarioService;
    private final UsuarioRepository usuarioRepository;
    private final PacienteRepository pacienteRepository;
    private final AgendamentoRepository agendamentoRepository;

    public RecepcaoPacienteService(UsuarioService usuarioService,
                                   UsuarioRepository usuarioRepository,
                                   PacienteRepository pacienteRepository,
                                   AgendamentoRepository agendamentoRepository) {
        this.usuarioService = usuarioService;
        this.usuarioRepository = usuarioRepository;
        this.pacienteRepository = pacienteRepository;
        this.agendamentoRepository = agendamentoRepository;
    }

    /** Todos os pacientes (titulares e dependentes) com a última consulta e o próximo exame. */
    @Transactional(readOnly = true)
    public List<PacienteRecepcaoResponseDTO> listar() {
        OffsetDateTime agora = OffsetDateTime.now();
        Map<Long, OffsetDateTime> ultimas = new HashMap<>();
        Map<Long, OffsetDateTime> proximos = new HashMap<>();
        Map<Long, Integer> totais = new HashMap<>();

        for (Agendamento a : agendamentoRepository.listarTodos()) {
            Long pacienteId = a.getPaciente().getId();
            totais.merge(pacienteId, 1, Integer::sum);

            OffsetDateTime data = a.getDataConfirmada();
            if (data == null) {
                continue;
            }
            if ("CONCLUIDO".equals(a.getStatus())) {
                ultimas.merge(pacienteId, data, (x, y) -> x.isAfter(y) ? x : y);
            } else if ("CONFIRMADO".equals(a.getStatus()) && data.isAfter(agora)) {
                proximos.merge(pacienteId, data, (x, y) -> x.isBefore(y) ? x : y);
            }
        }

        return pacienteRepository.listarTodosComResponsavel().stream()
                .map(p -> mapearParaRecepcao(p, ultimas.get(p.getId()), proximos.get(p.getId()),
                        totais.getOrDefault(p.getId(), 0)))
                .sorted(Comparator.comparing(PacienteRecepcaoResponseDTO::nome, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private PacienteRecepcaoResponseDTO mapearParaRecepcao(Paciente p,
                                                           OffsetDateTime ultima,
                                                           OffsetDateTime proximo,
                                                           int total) {
        Usuario responsavel = p.getUsuarioResponsavel();
        Endereco e = responsavel.getEndereco();
        EnderecoResponseDTO endereco = new EnderecoResponseDTO(
                e.getPublicId(), e.getCep(), e.getLogradouro(), e.getNumero(),
                e.getComplemento(), e.getBairro(), e.getCidade(), e.getEstado());

        // O titular herda os dados do usuário quando o registro do paciente não os repete
        return new PacienteRecepcaoResponseDTO(
                p.getPublicId(),
                p.getNome() != null ? p.getNome() : responsavel.getNome(),
                p.getCpf() != null ? p.getCpf() : responsavel.getCpf(),
                p.getDataNascimento() != null ? p.getDataNascimento() : responsavel.getDataNascimento(),
                p.getParentesco(),
                p.getConvenio(),
                responsavel.getTelefone(),
                responsavel.getEmail(),
                responsavel.getNome(),
                endereco,
                ultima,
                proximo,
                total);
    }

    /**
     * Cadastra um novo paciente (usuário PACIENTE + registro de paciente titular), usando as mesmas
     * regras do cadastro público, e devolve o titular para a recepção já poder agendar o exame.
     */
    @Transactional
    public PacienteResponseDTO cadastrar(UsuarioCadastroRequestDTO dto) {
        UsuarioResponseDTO criado = usuarioService.cadastrar(dto);

        Usuario usuario = usuarioRepository.findByPublicId(criado.publicId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuário recém-criado não encontrado."));
        Paciente titular = pacienteRepository
                .findByUsuarioResponsavelIdAndParentesco(usuario.getId(), "TITULAR")
                .orElseThrow(() -> new ResourceNotFoundException("Paciente titular não encontrado."));

        return new PacienteResponseDTO(
                titular.getPublicId(),
                titular.getNome(),
                titular.getCpf(),
                titular.getDataNascimento(),
                titular.getParentesco(),
                titular.getConvenio());
    }
}
