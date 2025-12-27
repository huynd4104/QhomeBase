-- Ensure seeded vehicle registrations use the standardized service type.

UPDATE card.register_vehicle
SET service_type = 'VEHICLE_REGISTRATION'
WHERE service_type = 'VEHICLE';












