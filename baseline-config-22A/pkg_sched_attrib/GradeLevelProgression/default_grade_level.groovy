//file:noinspection UnnecessaryQualifiedReference
@groovy.transform.BaseScript(com.oracle.sfp.scripting.api.sdk.autocomplete.GradeLevelProgressionScript)
package CHANGE_ME // this should be updated to your actual package name

import com.oracle.sfp.scripting.api.*
import com.oracle.sfp.scripting.api.util.*

def referenceDate = program.isTerm() ? term.getStartDate() : acy.getStartDate();

def totalPrevAcyUnits = acys
        .findAll { it.getNumber() < acyNo }
        .collect { it.getStatus() == "ACTUAL" ? it.getCompletedUnits() : it.getUnits() }
        .sum(0.0);

def totalCurAcyUnits = acy.getLoanPaymentPeriods()
        .findAll { it.getStatus() != "CANCELED" && it.getStartDate() < referenceDate }
        .collect { it.getUnits() }
        .sum(0.0);

def totalCreditsCompletedBeforeProgramStartDate = program.getCourses()
        .findAll {
                it.getSchedulingStatus() == "PASSED" &&
                        it.getEndDate() < actualStartDate &&
                        (it.getIncompleteResolutionDate() == null || it.getIncompleteResolutionDate() < actualStartDate)
        }
        .collect { it.getUnits() }
        .sum(0.0);

def totalCreditUnits = (program.getTotalAcceptedTransferUnits() +
        program.getAssessedUnits() +
        totalPrevAcyUnits +
        totalCurAcyUnits +
        totalCreditsCompletedBeforeProgramStartDate);

def creditPerAydUnit = totalCreditUnits / program.getAydUnits();

def baseGradeLevel = (program.isGraduate()
        ? 6
        : program.getProgramType() == "CERTIFICATE_POST_BACCALAUREATE"
        ? 5
        : 1);

def unboundedGradeLevel = baseGradeLevel + creditPerAydUnit.intValue();

def isAssociate = program.getProgramType() == "ASSOCIATE";

def gradeLevelUb = (isAssociate
        ? 2
        : program.isUndergraduate()
        ? 5
        : 7);

def gradeLevel = [unboundedGradeLevel, gradeLevelUb].min();

log.debug("actualStartDate={}, referenceDate={}, totalPrevAcyUnits={}, totalCurAcyUnits={}, totalCreditsCompletedBeforeProgramStartDate={}, totalCreditUnits={}, creditPerAydUnit={}, baseGradeLevel={}, isAssociate={}, gradeLevelUb={}, gradeLevel={}",
        actualStartDate,
        referenceDate,
        totalPrevAcyUnits,
        totalCurAcyUnits,
        totalCreditsCompletedBeforeProgramStartDate,
        totalCreditUnits,
        creditPerAydUnit,
        baseGradeLevel,
        isAssociate,
        gradeLevelUb,
        gradeLevel
);

return gradeLevel;