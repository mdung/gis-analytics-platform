package com.example.gis.util;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.operation.valid.IsValidOp;
import org.locationtech.jts.precision.GeometryPrecisionReducer;
import org.springframework.stereotype.Component;

@Component
public class GeometryValidator {
    private final GeometryFactory geometryFactory;

    public GeometryValidator() {
        this.geometryFactory = new GeometryFactory(new PrecisionModel(PrecisionModel.FLOATING), 4326);
    }

    /**
     * Validate geometry and return validation result
     */
    public ValidationResult validate(Geometry geometry) {
        if (geometry == null) {
            return ValidationResult.invalid("Geometry is null");
        }

        IsValidOp validOp = new IsValidOp(geometry);
        if (!validOp.isValid()) {
            return ValidationResult.invalid(validOp.getValidationError().getMessage());
        }

        return ValidationResult.valid();
    }

    /**
     * Normalize geometry: fix invalid geometries, reduce precision, remove duplicates
     */
    public Geometry normalize(Geometry geometry) {
        if (geometry == null) {
            return null;
        }

        Geometry normalized = geometry;

        // Fix invalid geometries
        if (!normalized.isValid()) {
            normalized = normalized.buffer(0); // Buffer(0) can fix some invalid geometries
        }

        // Reduce precision to avoid floating point issues
        PrecisionModel precisionModel = new PrecisionModel(PrecisionModel.FLOATING);
        GeometryPrecisionReducer reducer = new GeometryPrecisionReducer(precisionModel);
        normalized = reducer.reduce(normalized);

        // Remove duplicate coordinates
        normalized = normalized.union(); // Union with itself removes duplicates

        return normalized;
    }

    /**
     * Validate and normalize geometry
     */
    public Geometry validateAndNormalize(Geometry geometry) {
        Geometry normalized = normalize(geometry);
        ValidationResult result = validate(normalized);
        
        if (!result.isValid()) {
            throw new IllegalArgumentException("Geometry validation failed after normalization: " + result.getErrorMessage());
        }
        
        return normalized;
    }

    /**
     * Check if geometry is within valid bounds (WGS84: -180 to 180 longitude, -90 to 90 latitude)
     */
    public boolean isWithinBounds(Geometry geometry) {
        if (geometry == null) {
            return false;
        }

        org.locationtech.jts.geom.Envelope envelope = geometry.getEnvelopeInternal();
        return envelope.getMinX() >= -180 && envelope.getMaxX() <= 180 &&
               envelope.getMinY() >= -90 && envelope.getMaxY() <= 90;
    }

    public static class ValidationResult {
        private final boolean valid;
        private final String errorMessage;

        private ValidationResult(boolean valid, String errorMessage) {
            this.valid = valid;
            this.errorMessage = errorMessage;
        }

        public static ValidationResult valid() {
            return new ValidationResult(true, null);
        }

        public static ValidationResult invalid(String errorMessage) {
            return new ValidationResult(false, errorMessage);
        }

        public boolean isValid() {
            return valid;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }
}

