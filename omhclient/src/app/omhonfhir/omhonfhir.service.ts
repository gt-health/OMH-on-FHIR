import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { environment } from '../../environments/environment'

export interface DocumentReference{
  resourceType: string;
  status: string;
  type: DocumentReferenceType;
  indexed: string;
  content: DocumentReferenceContent[];
}
export interface DocumentReferenceType{
 text: string;
}
export interface DocumentReferenceContent{
  attachment: DocumentReferenceAttachment
}
export interface DocumentReferenceAttachment{
  contentType: string;
  url: string;
  title: string;
  creation: string;
}

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

  requestDocumentReference(shimmerId, startDate, endDate): Observable<DocumentReference>{
    var shimmerDocRefUrl = environment.omhOnFhirAPIBase + "/DocumentReference?subject=" + shimmerId;

    if(startDate){
      shimmerDocRefUrl = shimmerDocRefUrl + "&date=" + startDate;
    }
    if(endDate){
      shimmerDocRefUrl = shimmerDocRefUrl + "&date=" + endDate;
    }

    console.log("Requesting Document Reference " + shimmerDocRefUrl);

    //sample response
    //{
    // "resourceType":"DocumentReference",
    // "status":"current",
    // "type":{
    //    "text":"OMH fitbit data"
    // },
    // "indexed":"2018-07-31T22:02:11.408+00:00",
    // "content":[
    //     {
    //       "attachment":{
    //        "contentType":"application/json",
    //        "url":"Binary/1d1ddd60-0c42-4ed2-b0e3-8b43876ceb9b",
    //        "title":"OMH fitbit data",
    //        "creation":"2018-07-31T22:02:11+00:00"
    //       }
    //     }
    // ]
    //}
    //

    return this.http.get<DocumentReference>(shimmerDocRefUrl);
  }

  requestBinary(binaryUrl): Observable<Object>{
    var shimmerBinaryUrl = environment.omhOnFhirAPIBase + "/" + binaryUrl;
    return this.http.get(shimmerBinaryUrl);
  }
}
