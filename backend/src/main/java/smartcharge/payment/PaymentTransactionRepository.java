package smartcharge.payment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {
    @Query("select t from PaymentTransaction t where t.session.request.user.id = :userId order by t.attemptedAt desc, t.id desc")
    List<PaymentTransaction> findForUser(@Param("userId") Long userId);
}
