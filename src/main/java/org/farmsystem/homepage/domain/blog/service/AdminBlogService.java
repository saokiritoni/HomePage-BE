package org.farmsystem.homepage.domain.blog.service;

import lombok.RequiredArgsConstructor;
import org.farmsystem.homepage.domain.blog.dto.response.BlogApprovalResponseDTO;
import org.farmsystem.homepage.domain.blog.dto.response.PendingBlogResponseDTO;
import org.farmsystem.homepage.domain.blog.entity.ApprovalStatus;
import org.farmsystem.homepage.domain.blog.entity.Blog;
import org.farmsystem.homepage.domain.user.entity.User;
import org.farmsystem.homepage.domain.blog.repository.BlogRepository;
import org.farmsystem.homepage.domain.user.repository.UserRepository;
import org.farmsystem.homepage.global.error.exception.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import static org.farmsystem.homepage.global.error.ErrorCode.*;

@Service
@Transactional
@RequiredArgsConstructor
public class AdminBlogService {

    private final BlogRepository blogRepository;
    private final UserRepository userRepository;

    /**
     * 블로그 승인
     */
    public BlogApprovalResponseDTO approveBlog(Long blogId, Long adminUserId) {
        Blog blog = blogRepository.findById(blogId)
                .orElseThrow(() -> new EntityNotFoundException(BLOG_NOT_FOUND));

        User admin = userRepository.findById(adminUserId)
                .orElseThrow(() -> new EntityNotFoundException(USER_NOT_FOUND));

        blog.approve(admin);

        return new BlogApprovalResponseDTO(
                blog.getBlogId(),
                blog.getLink(),
                blog.getApprovalStatus().name(),
                admin.getName(),
                blog.getApprovedAt()
        );
    }

    /**
     * 블로그 거절
     */
    public void rejectBlog(Long blogId, Long adminUserId) {
        Blog blog = blogRepository.findById(blogId)
                .orElseThrow(() -> new EntityNotFoundException(BLOG_NOT_FOUND));

        User admin = userRepository.findById(adminUserId)
                .orElseThrow(() -> new EntityNotFoundException(USER_NOT_FOUND));

        blog.reject(admin);
    }



    /**
     * 승인 대기 중인 블로그 목록 조회
     */
    @Transactional(readOnly = true)
    public List<PendingBlogResponseDTO> getPendingBlogs() {
        return blogRepository.findByApprovalStatus(ApprovalStatus.PENDING).stream()
                .map(blog -> new PendingBlogResponseDTO(
                        blog.getBlogId(),
                        blog.getLink(),
                        blog.getUser().getName(),
                        blog.getCreatedAt()
                ))
                .toList();
    }
}
