package com.facebook.presto.sql.analyzer;

import static com.google.common.base.MoreObjects.toStringHelper;
import static java.util.Objects.requireNonNull;

/**
 * @author findepi
 * @since 2017-10-03
 */
public final class ResolvedReference
{
    public enum ReferenceType
    {
        COLUMN_REFERENCE, LAMBDA_ARGUMENT
    }

    private final FieldId fieldId;
    private final ReferenceType referenceType;

    public ResolvedReference(FieldId fieldId, ReferenceType referenceType)
    {
        this.fieldId = requireNonNull(fieldId, "fieldId is null");
        this.referenceType = requireNonNull(referenceType, "referenceType is null");
    }

    public FieldId getFieldId()
    {
        return fieldId;
    }

    public ReferenceType getReferenceType()
    {
        return referenceType;
    }

    @Override
    public String toString()
    {
        return toStringHelper(this)
                .add("fieldId", fieldId)
                .add("referenceType", referenceType)
                .toString();
    }
}
