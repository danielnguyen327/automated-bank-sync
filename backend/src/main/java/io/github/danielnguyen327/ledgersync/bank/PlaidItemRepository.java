package io.github.danielnguyen327.ledgersync.bank;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlaidItemRepository extends JpaRepository<PlaidItem, UUID> {

    List<PlaidItem> findByUserIdOrderByCreatedAt(UUID userId);

    /** Finds a bank only if it belongs to this user, so nobody can reach another user's bank. */
    Optional<PlaidItem> findByIdAndUserId(UUID id, UUID userId);
}
