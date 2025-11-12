-- Seed default users
-- Password hash for 'admin123' (BCrypt, rounds=10)
INSERT INTO users (id, username, password_hash, email, role) VALUES
    (uuid_generate_v4(), 'admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'admin@gis.local', 'ADMIN'),
    (uuid_generate_v4(), 'editor', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'editor@gis.local', 'EDITOR'),
    (uuid_generate_v4(), 'viewer', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'viewer@gis.local', 'VIEWER')
ON CONFLICT (username) DO NOTHING;

-- Seed layers
DO $$
DECLARE
    poi_layer_id UUID;
    roads_layer_id UUID;
    zones_layer_id UUID;
    admin_user_id UUID;
    i INTEGER;
    lng DOUBLE PRECISION;
    lat DOUBLE PRECISION;
    point_geom GEOMETRY;
    line_geom GEOMETRY;
    poly_geom GEOMETRY;
BEGIN
    -- Get admin user ID
    SELECT id INTO admin_user_id FROM users WHERE username = 'admin' LIMIT 1;

    -- Create POI layer
    INSERT INTO layers (id, code, name, geom_type, srid, style, metadata, created_by)
    VALUES (
        uuid_generate_v4(),
        'poi',
        'Points of Interest',
        'POINT',
        4326,
        '{"color": "#3b82f6", "size": 8, "icon": "marker", "iconColor": "#ffffff"}'::jsonb,
        '{"fields": [{"name": "name", "type": "string"}, {"name": "category", "type": "string"}, {"name": "rating", "type": "number"}]}'::jsonb,
        admin_user_id
    ) RETURNING id INTO poi_layer_id;

    -- Create Roads layer
    INSERT INTO layers (id, code, name, geom_type, srid, style, metadata, created_by)
    VALUES (
        uuid_generate_v4(),
        'roads',
        'Roads Network',
        'LINE',
        4326,
        '{"color": "#ef4444", "lineWidth": 3, "lineCap": "round"}'::jsonb,
        '{"fields": [{"name": "name", "type": "string"}, {"name": "type", "type": "string"}, {"name": "length_km", "type": "number"}]}'::jsonb,
        admin_user_id
    ) RETURNING id INTO roads_layer_id;

    -- Create Zones layer
    INSERT INTO layers (id, code, name, geom_type, srid, style, metadata, created_by)
    VALUES (
        uuid_generate_v4(),
        'zones',
        'Planning Zones',
        'POLYGON',
        4326,
        '{"color": "#10b981", "fillOpacity": 0.3, "strokeWidth": 2}'::jsonb,
        '{"fields": [{"name": "name", "type": "string"}, {"name": "zone_type", "type": "string"}, {"name": "area_ha", "type": "number"}]}'::jsonb,
        admin_user_id
    ) RETURNING id INTO zones_layer_id;

    -- Generate ~500 random POI points around Ho Chi Minh City area (10.7, 106.6)
    FOR i IN 1..500 LOOP
        lng := 106.6 + (random() * 0.5 - 0.25); -- ~105.85 to 107.35
        lat := 10.7 + (random() * 0.5 - 0.25);  -- ~10.45 to 10.95
        point_geom := ST_SetSRID(ST_MakePoint(lng, lat), 4326);
        
        INSERT INTO features (layer_id, properties, geom, created_by)
        VALUES (
            poi_layer_id,
            jsonb_build_object(
                'name', 'POI ' || i,
                'category', CASE (i % 5)
                    WHEN 0 THEN 'restaurant'
                    WHEN 1 THEN 'store'
                    WHEN 2 THEN 'hospital'
                    WHEN 3 THEN 'school'
                    ELSE 'park'
                END,
                'rating', round((random() * 4 + 1)::numeric, 1)
            ),
            point_geom,
            admin_user_id
        );
    END LOOP;

    -- Generate ~50 road segments
    FOR i IN 1..50 LOOP
        lng := 106.6 + (random() * 0.4 - 0.2);
        lat := 10.7 + (random() * 0.4 - 0.2);
        
        line_geom := ST_SetSRID(
            ST_MakeLine(
                ST_MakePoint(lng, lat),
                ST_MakePoint(lng + (random() * 0.05 - 0.025), lat + (random() * 0.05 - 0.025))
            ),
            4326
        );
        
        INSERT INTO features (layer_id, properties, geom, created_by)
        VALUES (
            roads_layer_id,
            jsonb_build_object(
                'name', 'Road ' || i,
                'type', CASE (i % 4)
                    WHEN 0 THEN 'highway'
                    WHEN 1 THEN 'arterial'
                    WHEN 2 THEN 'local'
                    ELSE 'alley'
                END,
                'length_km', round((ST_Length(geography(line_geom)) / 1000)::numeric, 2)
            ),
            line_geom,
            admin_user_id
        );
    END LOOP;

    -- Generate ~10 planning zones (polygons)
    FOR i IN 1..10 LOOP
        lng := 106.6 + (random() * 0.3 - 0.15);
        lat := 10.7 + (random() * 0.3 - 0.15);
        
        -- Create a simple square polygon
        poly_geom := ST_SetSRID(
            ST_MakePolygon(
                ST_MakeLine(ARRAY[
                    ST_MakePoint(lng - 0.01, lat - 0.01),
                    ST_MakePoint(lng + 0.01, lat - 0.01),
                    ST_MakePoint(lng + 0.01, lat + 0.01),
                    ST_MakePoint(lng - 0.01, lat + 0.01),
                    ST_MakePoint(lng - 0.01, lat - 0.01)
                ])
            ),
            4326
        );
        
        INSERT INTO features (layer_id, properties, geom, created_by)
        VALUES (
            zones_layer_id,
            jsonb_build_object(
                'name', 'Zone ' || i,
                'zone_type', CASE (i % 3)
                    WHEN 0 THEN 'residential'
                    WHEN 1 THEN 'commercial'
                    ELSE 'industrial'
                END,
                'area_ha', round((ST_Area(geography(poly_geom)) / 10000)::numeric, 2)
            ),
            poly_geom,
            admin_user_id
        );
    END LOOP;

    -- Create a sample geofence
    INSERT INTO geofences (name, description, geom, active, created_by)
    VALUES (
        'Central District',
        'Main business district geofence',
        ST_SetSRID(
            ST_MakePolygon(
                ST_MakeLine(ARRAY[
                    ST_MakePoint(106.65, 10.75),
                    ST_MakePoint(106.7, 10.75),
                    ST_MakePoint(106.7, 10.8),
                    ST_MakePoint(106.65, 10.8),
                    ST_MakePoint(106.65, 10.75)
                ])
            ),
            4326
        ),
        true,
        admin_user_id
    );

    -- Create sample devices
    INSERT INTO devices (code, name, last_position)
    VALUES
        ('DEV001', 'Device 1', ST_SetSRID(ST_MakePoint(106.67, 10.77), 4326)),
        ('DEV002', 'Device 2', ST_SetSRID(ST_MakePoint(106.68, 10.78), 4326)),
        ('DEV003', 'Device 3', ST_SetSRID(ST_MakePoint(106.69, 10.76), 4326));

END $$;

