import { defineStore } from 'pinia';
import { GeoFeatures } from 'src/components/models';
import { api } from 'boot/axios'

export const geoFeaturesStore = defineStore('geoFeatures', {
  state: () => ({
    geoFeatures: {} as GeoFeaturesMap,
  }),
  getters: {
      getGeoFeatures: (state) => state.geoFeatures,
      getGeoFeaturesById: (state) => {
          return (queryId: string) => state.geoFeatures[queryId];
      }
  },
  actions: {
    async getGeoFeaturesForQuery(query: string) {
        api.get('/api/backend/' + query)
        .then((response) => {
            this.geoFeatures[query] = response.data;
        })
        .catch(() => {
            console.log("Something went wrong?");
        })
    }
  },
});

interface GeoFeaturesMap {
    [queryId: string]: GeoFeatures;
}