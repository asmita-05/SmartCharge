package smartcharge.payment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findBySessionId(Long sessionId);

    @org.springframework.data.jpa.repository.Query("select p from Payment p where p.session.request.user.id = :userId order by p.paymentTime desc")
    List<Payment> findForUser(@org.springframework.data.repository.query.Param("userId") Long userId);
}
