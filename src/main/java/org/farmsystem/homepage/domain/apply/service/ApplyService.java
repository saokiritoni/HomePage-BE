package org.farmsystem.homepage.domain.apply.service;

import lombok.RequiredArgsConstructor;
import org.farmsystem.homepage.domain.apply.dto.*;
import org.farmsystem.homepage.domain.apply.dto.request.ApplyRequestDTO;
import org.farmsystem.homepage.domain.apply.dto.request.CreateApplyRequestDTO;
import org.farmsystem.homepage.domain.apply.dto.request.LoadApplyRequestDTO;
import org.farmsystem.homepage.domain.apply.dto.response.*;
import org.farmsystem.homepage.domain.apply.entity.*;
import org.farmsystem.homepage.domain.apply.repository.*;
import org.farmsystem.homepage.domain.common.entity.Track;
import org.farmsystem.homepage.global.error.ErrorCode;
import org.farmsystem.homepage.global.error.exception.BusinessException;
import org.farmsystem.homepage.global.error.exception.EntityNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ApplyService {

    private final QuestionRepository questionRepository;
    private final ApplyRepository applyRepository;
    private final AnswerRepository answerRepository;
    private final ChoiceRepository choiceRepository;
    private final AnswerChoiceRepository answerChoiceRepository;
    private final PasswordEncoder passwordEncoder;
    private final ApplyStatusRepository applyStatusRepository;

    // 전체 질문 조회
    public List<QuestionDTO> getQuestions() {
        List<Question> questions = questionRepository.findAllByOrderByTrackAscPriorityAsc();
        return questions.stream()
                .map(QuestionDTO::from)
                .collect(Collectors.toList());
    }

    // 지원서 생성
    @Transactional
    public CreateApplyResponseDTO createApply(CreateApplyRequestDTO request) {
        List<Apply> applies = applyRepository.findAllByStudentNumber(request.studentNumber());
        if (applies.stream().anyMatch(apply -> passwordEncoder.matches(request.password(), apply.getPassword()))) {
            throw new BusinessException(ErrorCode.APPLY_ALREADY_EXIST);
        }
        Apply apply = Apply.builder()
                .studentNumber(request.studentNumber())
                .password(passwordEncoder.encode(request.password()))
                .build();
        Apply savedApply = applyRepository.save(apply);
        return CreateApplyResponseDTO.builder()
                .applyId(savedApply.getApplyId())
                .build();
    }

    // 지원서 임시저장/제출
    @Transactional
    public ApplyResponseDTO saveApply(ApplyRequestDTO request, boolean submitFlag) {
        Apply apply = applyRepository.findById(request.applyId())
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.APPLY_NOT_FOUND));
        if (applyStatusRepository.existsByStudentNumber(apply.getStudentNumber())) {
            throw new BusinessException(ErrorCode.APPLY_ALREADY_SUBMITTED);
        }
        handleApplyInfo(apply, request);
        switch (apply.getStatus()) {
            case DRAFT -> handleDraftStatus(apply, request);
            case SAVED -> handleSavedStatus(apply, request);
        }
        handleApplyStatus(apply, submitFlag);
        return ApplyResponseDTO.builder()
                .applyId(apply.getApplyId())
                .build();
    }

    // 지원서 불러오기
    public LoadApplyResponseDTO loadApply(LoadApplyRequestDTO request) {
        List<Apply> applies = applyRepository.findAllByStudentNumber(request.studentNumber());
        Apply apply = applies.stream()
                .filter(applyItem -> passwordEncoder.matches(request.password(), applyItem.getPassword()))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.APPLY_INVALID_PASSWORD));
        return LoadApplyResponseDTO.from(apply);
    }

    // 관리자 - 지원서 목록
    public List<ApplyListResponseDTO> getApplyList(Track track) {
        List<Apply> applyList;
        if (track == null) {
            applyList = applyRepository.findAllSubmitted();
        } else {
            applyList = applyRepository.findAllSubmittedByTrack(track);
        }
        return applyList.stream()
                .map(ApplyListResponseDTO::from)
                .collect(Collectors.toList());
    }

    // 관리자 - 지원서 상세
    public SubmittedApplyResponseDTO getApply(Long applyId) {
        Apply apply = applyRepository.findById(applyId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.APPLY_NOT_FOUND));
        return SubmittedApplyResponseDTO.from(apply);
    }

    // 지원서 상태 처리
    private void handleApplyStatus(Apply apply, boolean isSubmit) {
        if (isSubmit) {
            applyStatusRepository.save(new ApplyStatus(apply.getStudentNumber()));
            apply.updateStatus(ApplyStatusEnum.SUBMITTED);
        } else if (apply.getStatus() == ApplyStatusEnum.DRAFT) {
            apply.updateStatus(ApplyStatusEnum.SAVED);
        }
    }

    // 지원서 개인정보 업데이트
    private void handleApplyInfo(Apply apply, ApplyRequestDTO request) {
        apply.updateName(request.name());
        apply.updateMajor(request.major());
        apply.updatePhoneNumber(request.phoneNumber());
        apply.updateEmail(request.email());
        apply.updateTrack(request.track());
    }

    // 최초작성 상태에서 저장/제출 처리
    private void handleDraftStatus(Apply apply, ApplyRequestDTO request) {
        for (AnswerDTO answer : request.answers()) {
            Question question = questionRepository.findById(answer.questionId())
                    .orElseThrow(() -> new EntityNotFoundException(ErrorCode.QUESTION_NOT_FOUND));
            Answer savedAnswer = answerRepository.save(Answer.builder()
                    .content(answer.content())
                    .apply(apply)
                    .question(question)
                    .build());
            saveAnswerChoices(answer, savedAnswer);
        }
    }

    // 임시저장 상태에서 저장/제출 처리
    private void handleSavedStatus(Apply apply, ApplyRequestDTO request) {
        for (AnswerDTO answer : request.answers()) {
            Answer savedAnswer = answerRepository.findByApplyApplyIdAndQuestionQuestionId(request.applyId(), answer.questionId())
                    .orElseGet(() -> {
                        Question question = questionRepository.findById(answer.questionId())
                                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.QUESTION_NOT_FOUND));
                        Answer newAnswer = Answer.builder()
                                .apply(apply)
                                .question(question)
                                .build();
                        return answerRepository.save(newAnswer);
                    });
            savedAnswer.updateContent(answer.content());
            answerChoiceRepository.deleteByAnswerAnswerId(savedAnswer.getAnswerId());
            saveAnswerChoices(answer, savedAnswer);
        }
    }

    // 선택지 저장 처리
    private void saveAnswerChoices(AnswerDTO answer, Answer savedAnswer) {
        if (answer.choiceId() != null) {
            for (Long choiceId : answer.choiceId()) {
                Choice choice = choiceRepository.findById(choiceId)
                        .orElseThrow(() -> new EntityNotFoundException(ErrorCode.CHOICE_NOT_FOUND));
                AnswerChoice answerChoice = AnswerChoice.builder()
                        .id(AnswerChoiceId.builder()
                                .answerId(savedAnswer.getAnswerId())
                                .choiceId(choiceId)
                                .build()
                        )
                        .answer(savedAnswer)
                        .choice(choice)
                        .build();
                answerChoiceRepository.save(answerChoice);
            }
        }
    }
}
