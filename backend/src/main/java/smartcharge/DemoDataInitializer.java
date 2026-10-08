package smartcharge;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import smartcharge.station.Charger;
import smartcharge.station.ChargerRepository;
import smartcharge.station.Station;
import smartcharge.station.StationRepository;
import smartcharge.user.User;
import smartcharge.user.UserRepository;
import smartcharge.user.Vehicle;
import smartcharge.user.VehicleRepository;

@Configuration
public class DemoDataInitializer {

    @Bean
    CommandLineRunner initializeDemoData(UserRepository users,
                                         VehicleRepository vehicles,
                                         StationRepository stations,
                                         ChargerRepository chargers,
                                         JdbcTemplate jdbcTemplate) {
        return args -> {
            // A request may have multiple historical sessions after priority preemption.
            jdbcTemplate.execute("""
                    DO $$ DECLARE constraint_row RECORD;
                    BEGIN
                      FOR constraint_row IN
                        SELECT tc.constraint_name
                        FROM information_schema.table_constraints tc
                        JOIN information_schema.key_column_usage kcu
                          ON tc.constraint_name = kcu.constraint_name
                         AND tc.table_schema = kcu.table_schema
                        WHERE tc.table_name = 'charging_sessions'
                          AND tc.constraint_type = 'UNIQUE'
                          AND kcu.column_name = 'request_id'
                      LOOP
                        EXECUTE format('ALTER TABLE charging_sessions DROP CONSTRAINT %I', constraint_row.constraint_name);
                      END LOOP;
                    END $$
                    """);
            User user = users.findByEmail("test@smartcharge.com")
                    .orElseGet(() -> users.save(new User("Test User", "test@smartcharge.com", "123456", "USER")));
            users.findByEmail("operator@smartcharge.com")
                    .orElseGet(() -> users.save(new User("Station Operator", "operator@smartcharge.com", "123456", "OPERATOR")));
            users.findByEmail("admin@smartcharge.com")
                    .orElseGet(() -> users.save(new User("SmartCharge Admin", "admin@smartcharge.com", "123456", "ADMIN")));

            if (vehicles.findByRegistrationNumber("UK07AB1234").isEmpty()) {
                vehicles.save(new Vehicle("UK07AB1234", "Tata Nexon EV", "SUV", 50.0, user));
            }
            if (stations.count() == 0) {
                stations.save(new Station("Graphic Era Charging Station", "Clement Town, Dehradun", "ACTIVE"));
            }
            if (chargers.count() == 0) {
                Station station = stations.findAll().get(0);
                chargers.save(new Charger("Fast Charger", "CCS2", 30.0, "AVAILABLE", station));
            }
        };
    }
}
