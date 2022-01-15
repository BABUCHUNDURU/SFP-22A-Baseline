//file:noinspection UnnecessaryQualifiedReference
@groovy.transform.BaseScript(com.oracle.sfp.scripting.api.sdk.autocomplete.AwardOverridingCriteriaScript)
package CHANGE_ME // this should be updated to your actual package name

import com.oracle.sfp.scripting.api.*
import com.oracle.sfp.scripting.api.util.*

/* Baseline - Award Overriding Criteria for PLUS & GPLUS - combined Term & Non-Term */
import org.joda.time.LocalDate;

optimizer.setFundingForPeriodFilter {fundCode, fundType -> fundCode != "DISCOUNT"};

if (program.getEnrollmentStatus() == "X" || pp.getStatus() == "CANCELED")
{
    return awardInfo.withMaxAmount(0).withRetainedAmount(0);
}
if (program.isTerm())
{
    def overlappingTerms = primaryProgram.getTerms().getOverlappingWith(term);
    /* Methods returning collection, as well as filters, will be plural; otherwise singular */
    if (overlappingTerms.getStudentsTermStatuses().contains("WITHDRAWN")) 
    {
        log.debug("AWARD FREEZING CRITERIA - STUDENT TERM IS WITHDRAWN");

        def r2t4OverlappingTerms = r2t4.getOverlappingWith(term)
                .getWithProcessStatuses(["NOT_REQUIRED", "COMPLETED"])
                .getWithOldas(overlappingTerms.getOldas());

        if (!r2t4OverlappingTerms.isEmpty())
        {
            log.debug("AWARD FREEZING CRITERIA - R2T4 Overlapping Terms is not empty, FREEZING!");

            def sum = disbursements.getInAcademicYear(acyNo)
                    .getWithFundCode(fundCode)
                    .getInAwardYear(awy)
                    .getInLoanPeriod(lpNo)
                    .getInPaymentPeriod(ppNo)
                    .filterStatuses { status -> status != "DISBURSEMENT_CANCELED" }
                    .getPpMaxDisbursementAmount();

            return awardInfo.withMaxAmount(sum).withRetainedAmount(sum);
        } 
        else
        {
            log.debug("AWARD FREEZING CRITERIA - R2T4 PROCESS NOT YET IN A FREEZING STATE");
        }
    }

    if (enrollmentStatus == "NOT_ATTENDING" || enrollmentStatus == "LESS_THAN_HALF_TIME")
    {
		log.debug("ZEROING - enrollmentStatus={} ", enrollmentStatus);
        return awardInfo.withMaxAmount(0).withRetainedAmount(0);
    }

	def freezingDateForStandard = acy.getTerms()
		.filter {t -> t.isSummerTrailer() && t.isAttending() } // from summer trailers (probably at most 1)
		.min() // get the earliest (yields an optional value)
		.map {t -> t.getStartDate()} // map optional to its start date
		.getOrElse(lp.getEndDate()); // if optional absent (no summer trailers) take lp.getEndDate

	def now = LocalDate.now();

    if (log.debug(lp.getEndDate() <= now, "LOAN PERIOD IN THE PAST") 
    	|| log.debug(term.isStandard() && freezingDateForStandard <= now, "SUMMER TRAILER STARTING")
    	|| log.debug(term.isSummerHeader(), "SUMMER HEADER")
    	|| log.debug(term.isSummerTrailer() && now < freezingDateForStandard, "TOO EARLY FOR SUMMER TRAILER"))
    {
        def sum = disbursements.getInAcademicYear(acyNo)
               .getWithFundCode(fundCode)
               .getInAwardYear(awy)
               .getInLoanPeriod(lpNo)
               .getInPaymentPeriod(ppNo)
               .filterStatuses { status -> status != "DISBURSEMENT_CANCELED" }
               .getTotalDisbursementAmount();
    	log.debug("FREEZING TO {}", sum);               
       	return awardInfo.withMaxAmount(sum).withRetainedAmount(sum);
    }
}
return awardInfo;