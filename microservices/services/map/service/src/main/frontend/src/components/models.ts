export interface Todo {
  id: number;
  content: string;
}

export interface Meta {
  totalCount: number;
}

export interface GeoFeatures {
  geoByField: GeoByField;
  functions: GeoFunction[];
}

export interface GeoByField {
  [key: string]: GeoTerms; 
}

export interface GeoTerms {
  type: string;
  geo?: Geo;
  geoByTier?: GeoByTier;
}

export interface GeoByTier {
  [key: string]: Geo;
}

export interface Geo {
  wkt: string;
  geoJson: Object;
}

export interface GeoFunction {
  function: string;
  fields: string[];
  geoJson: Object;
}
