package io.slingr.endpoints.hl7.customValidators;

import ca.uhn.hl7v2.Version;
import ca.uhn.hl7v2.validation.builder.support.DefaultValidationBuilder;

public class Hl7v281ValidationBuilder extends DefaultValidationBuilder {

    @Override
    protected void configure() {
        super.configure();
        forVersion(Version.V281)
                //Validation for ADT_A01
                .message("ADT", "A01")
                .terser("PID-3-1", not(empty()))
                .terser("PID-3-5", not(empty()))
                .terser("PID-5-1", not(empty()));
    }
}
