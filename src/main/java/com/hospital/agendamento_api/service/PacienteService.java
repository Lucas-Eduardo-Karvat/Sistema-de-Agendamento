package com.hospital.agendamento_api.service;

import com.hospital.agendamento_api.dto.DependenteRequestDTO;
import com.hospital.agendamento_api.dto.PacienteResponseDTO;
import com.hospital.agendamento_api.entity.Paciente;
import com.hospital.agendamento_api.entity.Usuario;
import com.hospital.agendamento_api.exception.BusinessException;
import com.hospital.agendamento_api.exception.ResourceNotFoundException;
import com.hospital.agendamento_api.repository.AgendamentoRepository;
import com.hospital.agendamento_api.repository.PacienteRepository;
import com.hospital.agendamento_api.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class PacienteService {

    // Valores aceitos pela constraint chk_pacientes_parentesco (TITULAR é só o próprio usuário)
    private static final Set<String> PARENTESCOS_DEPENDENTE = Set.of("FILHO", "CONJUGE", "PAI_MAE", "OUTRO");

    private final PacienteRepository pacienteRepository;
    private final UsuarioRepository usuarioRepository;
    private final AgendamentoRepository agendamentoRepository;

    public PacienteService(PacienteRepository pacienteRepository,
                           UsuarioRepository usuarioRepository,
                           AgendamentoRepository agendamentoRepository) {
        this.pacienteRepository = pacienteRepository;
        this.usuarioRepository = usuarioRepository;
        this.agendamentoRepository = agendamentoRepository;
    }

    @Transactional
    public PacienteResponseDTO cadastrarDependente(DependenteRequestDTO dto, String emailResponsavel) {
        Usuario responsavel = buscarResponsavel(emailResponsavel);

        String parentesco = normalizarParentesco(dto.parentesco());
        String cpf = normalizarCpf(dto.cpf());
        validarDataNascimento(dto.dataNascimento());
        if (cpf != null && pacienteRepository.existsByCpfAndAtivoTrue(cpf)) {
            throw new BusinessException("Já existe um paciente cadastrado com este CPF.");
        }

        Paciente dependente = new Paciente();
        dependente.setUsuarioResponsavel(responsavel);
        dependente.setNome(dto.nome().trim());
        dependente.setCpf(cpf);
        dependente.setDataNascimento(dto.dataNascimento());
        dependente.setParentesco(parentesco);
        dependente.setConvenio(convenioOuPadrao(dto.convenio()));

        return mapearParaDTO(pacienteRepository.save(dependente));
    }

    /** Altera os dados de um dependente do próprio usuário (o titular é alterado em "Meu perfil"). */
    @Transactional
    public PacienteResponseDTO atualizarDependente(UUID publicId, DependenteRequestDTO dto, String emailResponsavel) {
        Paciente dependente = buscarDependenteDoUsuario(publicId, emailResponsavel);

        String parentesco = normalizarParentesco(dto.parentesco());
        String cpf = normalizarCpf(dto.cpf());
        validarDataNascimento(dto.dataNascimento());
        if (cpf != null && pacienteRepository.existsByCpfAndAtivoTrueAndIdNot(cpf, dependente.getId())) {
            throw new BusinessException("Já existe um paciente cadastrado com este CPF.");
        }

        dependente.setNome(dto.nome().trim());
        dependente.setCpf(cpf);
        dependente.setDataNascimento(dto.dataNascimento());
        dependente.setParentesco(parentesco);
        dependente.setConvenio(convenioOuPadrao(dto.convenio()));

        return mapearParaDTO(pacienteRepository.save(dependente));
    }

    /**
     * Remove um dependente da conta. É uma inativação: o histórico de exames fica guardado no hospital,
     * mas o paciente some das telas do responsável. Não é permitido se ainda houver exame pendente/confirmado.
     */
    @Transactional
    public void removerDependente(UUID publicId, String emailResponsavel) {
        Paciente dependente = buscarDependenteDoUsuario(publicId, emailResponsavel);

        if (agendamentoRepository.existsByPacienteIdAndStatusIn(dependente.getId(), List.of("PENDENTE", "CONFIRMADO"))) {
            throw new BusinessException(
                    "Este paciente tem exames pendentes ou confirmados. Cancele-os antes de removê-lo.");
        }

        dependente.setAtivo(false);
        pacienteRepository.save(dependente);
    }

    @Transactional(readOnly = true)
    public List<PacienteResponseDTO> listarMeusPacientes(String emailResponsavel) {
        Usuario responsavel = buscarResponsavel(emailResponsavel);

        return pacienteRepository.findByUsuarioResponsavelIdAndAtivoTrue(responsavel.getId())
                .stream()
                .map(this::mapearParaDTO)
                .toList();
    }

    private Usuario buscarResponsavel(String email) {
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário responsável não encontrado"));
    }

    private Paciente buscarDependenteDoUsuario(UUID publicId, String emailResponsavel) {
        Usuario responsavel = buscarResponsavel(emailResponsavel);

        Paciente paciente = pacienteRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Paciente não encontrado"));

        // Só enxerga os próprios pacientes ativos (mesma resposta de "não encontrado" para não vazar a existência)
        boolean doUsuario = paciente.getUsuarioResponsavel().getId().equals(responsavel.getId());
        if (!doUsuario || !Boolean.TRUE.equals(paciente.getAtivo())) {
            throw new ResourceNotFoundException("Paciente não encontrado");
        }
        if ("TITULAR".equals(paciente.getParentesco())) {
            throw new BusinessException("Os dados do titular são alterados em Meu perfil.");
        }
        return paciente;
    }

    private String normalizarParentesco(String valor) {
        String parentesco = valor == null ? "" : valor.trim().toUpperCase();
        if (!PARENTESCOS_DEPENDENTE.contains(parentesco)) {
            throw new BusinessException("Parentesco inválido. Use: FILHO, CONJUGE, PAI_MAE ou OUTRO.");
        }
        return parentesco;
    }

    // CPF é opcional para dependente; quando informado precisa ter 11 números
    private String normalizarCpf(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        String digitos = valor.replaceAll("\\D", "");
        if (digitos.length() != 11) {
            throw new BusinessException("CPF deve conter exatamente 11 números (ou deixe em branco).");
        }
        return digitos;
    }

    private void validarDataNascimento(LocalDate data) {
        if (data.isAfter(LocalDate.now())) {
            throw new BusinessException("A data de nascimento não pode ser futura.");
        }
    }

    private String convenioOuPadrao(String convenio) {
        return convenio != null && !convenio.isBlank() ? convenio.trim() : "Particular";
    }

    private PacienteResponseDTO mapearParaDTO(Paciente p) {
        return new PacienteResponseDTO(
                p.getPublicId(),
                p.getNome(),
                p.getCpf(),
                p.getDataNascimento(),
                p.getParentesco(),
                p.getConvenio()
        );
    }
}
