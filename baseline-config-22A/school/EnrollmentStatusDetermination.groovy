//file:noinspection UnnecessaryQualifiedReference
@groovy.transform.BaseScript(com.oracle.sfp.scripting.api.sdk.autocomplete.GlobalEnrollmentStatusCriteriaScript)
package CHANGE_ME // this should be updated to your actual package name

import com.oracle.sfp.scripting.api.*
import com.oracle.sfp.scripting.api.util.*

/* Global Enrollment Status - SFP Baseline - Combined Term and Non-Term */

/*
def actualCourses = helper.filterCourses(program)
            .period(term)
            .safiCreatedDate(safiCreatedDate)
            .go();
*/

import org.joda.time.LocalDate;
            
if (program.isTerm())
{
    def countedUnits = 0.0;
    def safiTerm = program.getTerms().getOverlappingWith(term).get(0);
    
    def termStartPlus14 = term.getStartDate().plusDays(14);
    
    log.debug ("Term start date = {}, term start date + 14 = {}", term.getStartDate(), termStartPlus14);
    
    if ((program.getEnrollmentStatus() == "W" && program.getOfficialLastDateOfAttendance() <= term.getEndDate()) || safiTerm.getStudentsTermStatus() == "WITHDRAWN")
    {
        for (def course: program.getCourses().getAssociatedTo(period)) 
        {   
            if (["WITHDRAWN"].contains(course.getSchedulingStatus()))
            {
                if (course.getLastDateOfAttendance() != null && ((program.getOfficialLastDateOfAttendance() != null && course.getLastDateOfAttendance() == program.getOfficialLastDateOfAttendance()) || (safiTerm.getOlda() != null && course.getLastDateOfAttendance() == safiTerm.getOlda())) && course.getLastDateOfAttendance() > termStartPlus14)
                {
                    log.debug("GLOBAL ENROLLMENT SCRIPT - WITHDRAWN: courseLDA = {}, courseSchedulingStatus = {}, programOLDA = {}, courseUnitsToAdd = {}", course.getLastDateOfAttendance(), course.getSchedulingStatus(), program.getOfficialLastDateOfAttendance(), course.getUnits());
                    
                    countedUnits = countedUnits + course.getUnits();
                }
            }
            else if (["SCHEDULED", "PASSED", "FAILED"].contains(course.getSchedulingStatus()))
            {
                log.debug("GLOBAL ENROLLMENT SCRIPT - SCHEDULED PASSED FAILED: courseSchedulingStatus = {}, courseUnitsToAdd = {}", course.getSchedulingStatus(), course.getUnits());
                
                countedUnits = countedUnits + course.getUnits();
            }
        }
    }
    else
    {
        for (def course: program.getCourses().getAssociatedTo(period)) 
        {   
            log.debug("GLOBAL ENROLLMENT SCRIPT - NON WITHDRAWN, COURSE IN LOOP: courseEndDate = {}, courseAraIndicator = {}, courseSchedulingStatus = {}, courseUnitsToAdd = {}", course.getEndDate(), course.getFirstAraIndicator(), course.getSchedulingStatus(), course.getUnits());
    
           if ((safiCreatedDate <= course.getEndDate() || course.getFirstAraIndicator() || ["SCHEDULED", "ENROLLED"].contains(course.getSchedulingStatus()) || (["WITHDRAWN"].contains(course.getSchedulingStatus()) && course.getLastDateOfAttendance() != null && course.getLastDateOfAttendance() > termStartPlus14)))
            {
                countedUnits = countedUnits + course.getUnits();
            }
        }
    }

    def NA = 0.0;
    def LTHT = 6.0;
    def HT = 9.0;
    def TQT = 12.0;

    return helper.mapRanges()
                .le(NA, "NOT_ATTENDING")
                .lt(LTHT, "LESS_THAN_HALF_TIME")
                .lt(HT, "HALF_TIME")
                .lt(TQT, "THREE_QUARTER_TIME")
                .defaultValue("FULL_TIME")
                .apply(countedUnits);
}
return "FULL_TIME";