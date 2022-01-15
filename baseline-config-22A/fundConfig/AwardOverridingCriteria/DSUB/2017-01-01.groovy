//file:noinspection UnnecessaryQualifiedReference
@groovy.transform.BaseScript(com.oracle.sfp.scripting.api.sdk.autocomplete.AwardOverridingCriteriaScript)
package CHANGE_ME // this should be updated to your actual package name

import com.oracle.sfp.scripting.api.*
import com.oracle.sfp.scripting.api.util.*

/* Baseline - Award Overriding Criteria for DSUB DUNSUB - combined Term and Non-Term */
import org.joda.time.LocalDate

static <T> Closure<T> lazy(Closure<T> init) {
    T value
    boolean isInitDue = true
    return {
        if (isInitDue) {
            isInitDue = false
            value = init()
        }
        value
    }
}

def disbursementsInThisPp = lazy {
    disbursements.getInAcademicYear(acyNo)
            .getWithFundCode(fundCode)
            .getInAwardYear(awy)
            .getInLoanPeriod(lpNo)
            .getInPaymentPeriod(ppNo)
}

def nonCancelledDisbursementsInThisPp = lazy {
    disbursementsInThisPp()
            .getCancelled(false)
            .filterStatuses { status -> status != "DISBURSEMENT_CANCELED" }
}

def thisPpIsDisbursed = lazy {
    log.debug(nonCancelledDisbursementsInThisPp().getStatuses().contains("DISBURSED"), "DISBURSED PP")
}

def thisPpFundOutput = lazy {
    partialFundOutput.getInLoanPeriod(lpNo).getInPaymentPeriod(ppNo)
}

def thisPpIsProrated = lazy {
    log.debug(thisPpFundOutput().isEligible() && thisPpFundOutput().isProrated(), "PRORATED PP")
}

def isInNASuffix = term && term.standard && acy.terms.findAll { t -> t.startDate >= term.startDate && t.standard }
        .every { t -> t.notAttending }

optimizer
        .setCumulativeGradeLevelLimitApplicable(
                (!term || term.standard)
                        && (!isLastAcy || program.undergraduate)
                        && (!isLastAcy || !isInNASuffix)
                        && !thisPpIsProrated())
        .setFundingForPeriodFilter { fundCode, fundType -> fundCode != "DISCOUNT" }

if (program.getEnrollmentStatus() == "X" || pp.getStatus() == "CANCELED")
{
    return awardInfo.withMaxAmount(0.0).withRetainedAmount(0.0)
}
if (program.isTerm())
{
    def overlappingTerms = primaryProgram.getTerms().getOverlappingWith(term)
    /* Methods returning collection, as well as filters, will be plural; otherwise singular */
    if (overlappingTerms.getStudentsTermStatuses().contains("WITHDRAWN"))
    {
        log.debug("STUDENT TERM IS WITHDRAWN")

        def r2t4OverlappingTerms = r2t4.getOverlappingWith(term)
                .getWithProcessStatuses(["NOT_REQUIRED", "COMPLETED"])
                .getWithOldas(overlappingTerms.getOldas())

        if (!r2t4OverlappingTerms.isEmpty())
        {
            log.debug("R2T4 Overlapping Terms is not empty, FREEZING!")

            def sum = nonCancelledDisbursementsInThisPp().getPpMaxDisbursementAmount()

            return awardInfo.withMaxAmount(sum).withRetainedAmount(sum)
        }
        else
        {
            log.debug("R2T4 PROCESS NOT YET IN A FREEZING STATE")
        }
    }

    if (enrollmentStatus == "NOT_ATTENDING" || enrollmentStatus == "LESS_THAN_HALF_TIME")
    {
        log.debug("ZEROING - enrollmentStatus={} ", enrollmentStatus)
        return awardInfo.withMaxAmount(0.0).withRetainedAmount(0.0)
    }

    def freezingDateForStandard = acy.getTerms()
            .filter {t -> t.isSummerTrailer() && t.isAttending() }
            .min() // get the earliest (yields an optional value)
            .map {t -> t.getStartDate()} // map optional to its start date
            .getOrElse(lp.getEndDate()) // if optional absent (no summer trailers) take lp.getEndDate

    def now = LocalDate.now()

    if (log.debug(lp.getEndDate() <= now, "LOAN PERIOD IN THE PAST") // Update this line to determine when to freeze the standard terms and roll over unused funds to summer terms
            || log.debug(term.isStandard() && freezingDateForStandard <= now, "SUMMER TRAILER STARTING")
            || log.debug(term.isSummerHeader(), "SUMMER HEADER")
            || log.debug(term.isSummerTrailer() && now < freezingDateForStandard, "TOO EARLY FOR SUMMER TRAILER")
            || (thisPpIsProrated() && thisPpIsDisbursed()))
    {
        def sum = nonCancelledDisbursementsInThisPp().getTotalDisbursementAmount()
        log.debug("FREEZING TO {}", sum)
        return awardInfo.withMaxAmount(sum).withRetainedAmount(sum)
    }
}
return awardInfo