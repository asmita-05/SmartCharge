package smartcharge.payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
public interface BookingPaymentAttemptRepository extends JpaRepository<BookingPaymentAttempt, Long> {
    @Query("select a from BookingPaymentAttempt a where a.request.user.id = :userId order by a.attemptedAt desc, a.id desc")
    List<BookingPaymentAttempt> findForUser(@Param("userId") Long userId);
}
