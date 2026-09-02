package com.examiq.backend.service;

import com.examiq.backend.entity.Bookmark;
import com.examiq.backend.entity.Paper;
import com.examiq.backend.entity.User;
import com.examiq.backend.repository.BookmarkRepository;
import com.examiq.backend.repository.PaperRepository;
import com.examiq.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class BookmarkService {

    private final BookmarkRepository bookmarkRepository;
    private final PaperRepository paperRepository;
    private final UserRepository userRepository;

    public BookmarkService(BookmarkRepository bookmarkRepository, PaperRepository paperRepository, UserRepository userRepository) {
        this.bookmarkRepository = bookmarkRepository;
        this.paperRepository = paperRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Bookmark addBookmark(Long paperId, Long userId) {
        Paper paper = paperRepository.findById(paperId)
                .orElseThrow(() -> new IllegalArgumentException("Paper not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Optional<Bookmark> existingBookmark = bookmarkRepository.findByUserAndPaper(user, paper);
        if (existingBookmark.isPresent()) {
            throw new IllegalArgumentException("Paper already bookmarked");
        }

        Bookmark bookmark = new Bookmark();
        bookmark.setUser(user);
        bookmark.setPaper(paper);
        return bookmarkRepository.save(bookmark);
    }

    @Transactional
    public void removeBookmark(Long paperId, Long userId) {
        Paper paper = paperRepository.findById(paperId)
                .orElseThrow(() -> new IllegalArgumentException("Paper not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Bookmark bookmark = bookmarkRepository.findByUserAndPaper(user, paper)
                .orElseThrow(() -> new IllegalArgumentException("Bookmark not found"));

        bookmarkRepository.delete(bookmark);
    }

    public List<Bookmark> getUserBookmarks(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return bookmarkRepository.findByUserOrderByCreatedAtDesc(user);
    }

    public boolean isBookmarked(Long paperId, Long userId) {
        Paper paper = paperRepository.findById(paperId)
                .orElseThrow(() -> new IllegalArgumentException("Paper not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        return bookmarkRepository.findByUserAndPaper(user, paper).isPresent();
    }
}
