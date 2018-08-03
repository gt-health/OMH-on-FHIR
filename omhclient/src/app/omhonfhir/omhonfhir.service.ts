import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { environment } from '../../environments/environment'

const headerDict = {
  //'Content-Type': 'application/json',
  'Accept': 'application/json',
  //'Access-Control-Allow-Headers': 'Content-Type',
};
const requestOptions = {
  headers: new HttpHeaders(headerDict)
};

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

export interface Binary{
  resourceType: string;
  contentType: string;
  content: string;
}

export interface OmhActivity{
  shim: string;
  timeStamp: string;
  body: OmhActivityBody[];
}
export interface OmhActivityBody{
  body: OmhActivityBodyBody;
  header: OmhActivityHeader;
}
export interface OmhActivityBodyBody{
  activity_name: string;
  effective_time_frame: OmhActivityEffectiveTimeFrame;
}

export interface OmhActivityHeader{
  acquisition_provenance: OmhActivityAcquisitionProvenance;
  creation_date_time: string;
  id: string;
  schema_id: OmhActivitySchemaId;
}

export interface OmhActivityEffectiveTimeFrame{
  time_interval: OmhActivityTimeInterval;
}

export interface OmhActivityTimeInterval{
  end_date_time: string;
  start_date_time: string;
}

export interface OmhActivityAcquisitionProvenance{
  source_name: string;
  source_origin_id: string;
}

export interface OmhActivitySchemaId{
  name: string;
  namespace: string;
  version: string;
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

  //requestBinaryAsJson(binaryUrl): Observable<Binary>{
  requestBinaryAsJson(binaryUrl): Observable<OmhActivity>{
    //var shimmerBinaryUrl = environment.omhOnFhirAPIBase + "/" + binaryUrl;
    var shimmerBinaryUrl = "https://apps.hdap.gatech.edu/mdata/Binary/b365e896-86a8-4fa8-96b7-11ee9160b372";
    console.log("Requesting Binary " + shimmerBinaryUrl);
     return this.http.get<OmhActivity>(shimmerBinaryUrl, requestOptions );
  }

  requestBinaryAsBase64(binaryUrl): Observable<Binary>{
    var shimmerBinaryUrl = environment.omhOnFhirAPIBase + "/" + binaryUrl;
    return this.http.get<Binary>(shimmerBinaryUrl);
  }

}
