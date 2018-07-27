import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { environment } from '../../environments/environment'

@Injectable({
  providedIn: 'root'
})
export class OmhonfhirService {

  //TODO: probably store this in an external object to share
  patientId: string;

  constructor(private http: HttpClient) { }

  getPatietId(): string{
    console.log("Getting Patient ID");
    /** TODO: For now return hardcoded value, eventually return this.patientId from the SMART on FHIR server */
    return "3f6625db-8cc7-4d25-9bf4-9febdc7028cd";
  }

  requestDocumentReference(shimmerId, startDate, endDate): Observable<Object>{

    var shimmerDocRefUrl = environment.omhOnFhirAPIBase + "/DocumentReference?subject=" + shimmerId;

    if(startDate){
      shimmerDocRefUrl = shimmerDocRefUrl + "&date=" + startDate;
    }
    if(endDate){
      shimmerDocRefUrl = shimmerDocRefUrl + "&date=" + endDate;
    }

    console.log("Requesting Document Reference " + shimmerDocRefUrl);

    return this.http.get(shimmerDocRefUrl);
  }

  requestBinary(documentId): Observable<Object>{
    var shimmerBinaryUrl = environment.omhOnFhirAPIBase + "/Binary/" + documentId;
    return this.http.get<Object>(shimmerBinaryUrl);
  }
}
