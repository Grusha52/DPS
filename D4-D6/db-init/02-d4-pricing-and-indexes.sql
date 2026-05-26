-- Connect to the demo database
\c demo

SET search_path = bookings, public;

-- Task D4: Restore price information
CREATE TABLE public.pricing_rules (
    route_no text NOT NULL,
    fare_conditions text NOT NULL,
    price numeric(10,2) NOT NULL,
    PRIMARY KEY (route_no, fare_conditions)
);

INSERT INTO public.pricing_rules (route_no, fare_conditions, price)
SELECT 
    f.route_no, 
    s.fare_conditions, 
    MAX(s.price) as price
FROM bookings.flights f
JOIN bookings.segments s ON f.flight_id = s.flight_id
GROUP BY f.route_no, s.fare_conditions
ON CONFLICT (route_no, fare_conditions) DO NOTHING;

-- Task D6: Indexes for faster query execution
CREATE INDEX IF NOT EXISTS idx_flights_departure_arrival ON bookings.flights (scheduled_departure);
CREATE INDEX IF NOT EXISTS idx_segments_flight_id ON bookings.segments (flight_id);
CREATE INDEX IF NOT EXISTS idx_segments_ticket_no ON bookings.segments (ticket_no);
CREATE INDEX IF NOT EXISTS idx_tickets_book_ref ON bookings.tickets (book_ref);
