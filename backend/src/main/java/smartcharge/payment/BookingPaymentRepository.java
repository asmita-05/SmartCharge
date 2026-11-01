package smartcharge.payment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface BookingPaymentRepository extends JpaRepository<BookingPayment, Long> {
    Optional<BookingPayment> findByRequestId(Long requestId);
}
