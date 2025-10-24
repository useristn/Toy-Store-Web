package t4m.toy_store.support.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import t4m.toy_store.support.entity.SupportMessage;

import java.util.List;

@Repository
public interface SupportMessageRepository extends JpaRepository<SupportMessage, Long> {
    List<SupportMessage> findBySessionIdOrderByCreatedAtAsc(String sessionId);

    List<SupportMessage> findBySessionIdAndIsReadFalse(String sessionId);

    long countBySessionIdAndSenderTypeAndIsReadFalse(String sessionId, String senderType);
}
