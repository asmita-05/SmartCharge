CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(150) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(30) NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS vehicles (
    id BIGSERIAL PRIMARY KEY,
    registration_number VARCHAR(50) UNIQUE NOT NULL,
    model VARCHAR(100),
    vehicle_type VARCHAR(50),
    battery_capacity DOUBLE PRECISION NOT NULL,
    user_id BIGINT NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS stations (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    location VARCHAR(255),
    status VARCHAR(30) NOT NULL
);

CREATE TABLE IF NOT EXISTS chargers (
    id BIGSERIAL PRIMARY KEY,
    charger_type VARCHAR(50),
    connector_type VARCHAR(50) NOT NULL,
    power_kw DOUBLE PRECISION NOT NULL,
    status VARCHAR(30) NOT NULL,
    station_id BIGINT NOT NULL,
    FOREIGN KEY (station_id) REFERENCES stations(id)
);

CREATE TABLE IF NOT EXISTS charging_requests (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    vehicle_id BIGINT NOT NULL,
    current_battery DOUBLE PRECISION NOT NULL,
    target_battery DOUBLE PRECISION NOT NULL,
    estimated_minutes DOUBLE PRECISION,
    arrival_time TIMESTAMP NOT NULL,
    status VARCHAR(30) NOT NULL,
    priority VARCHAR(30) NOT NULL,
    emergency_requested BOOLEAN NOT NULL,
    emergency_status VARCHAR(30) NOT NULL,
    emergency_reason VARCHAR(500),
    required_connector_type VARCHAR(50),
    station_id BIGINT,
    requested_charger_id BIGINT,
    admin_approved BOOLEAN,
    admin_decision VARCHAR(30),

    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (vehicle_id) REFERENCES vehicles(id),
    FOREIGN KEY (station_id) REFERENCES stations(id),
    FOREIGN KEY (requested_charger_id) REFERENCES chargers(id)
);

CREATE TABLE IF NOT EXISTS charging_sessions (
    id BIGSERIAL PRIMARY KEY,
    request_id BIGINT NOT NULL,
    charger_id BIGINT NOT NULL,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP,
    status VARCHAR(30) NOT NULL,
    energy_used DOUBLE PRECISION,
    actual_minutes DOUBLE PRECISION,

    FOREIGN KEY (request_id) REFERENCES charging_requests(id),
    FOREIGN KEY (charger_id) REFERENCES chargers(id)
);

CREATE TABLE IF NOT EXISTS payments (
    id BIGSERIAL PRIMARY KEY,
    session_id BIGINT UNIQUE NOT NULL,
    amount DOUBLE PRECISION NOT NULL,
    status VARCHAR(30) NOT NULL,
    payment_reference VARCHAR(100),
    payment_method VARCHAR(50),
    payment_time TIMESTAMP NOT NULL,

    FOREIGN KEY (session_id) REFERENCES charging_sessions(id)
);
