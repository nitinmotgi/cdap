/*
 * Copyright © 2016 Cask Data, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

class TrackerResultsController {
  constructor($state, myTrackerApi, $scope, $q) {
    this.$state = $state;
    this.$scope = $scope;
    this.myTrackerApi = myTrackerApi;
    this.$q = $q;

    this.loading = false;
    this.entitiesShowAllButton = false;
    this.metadataShowAllButton = false;
    this.currentPage = 1;
    this.fullResults = [];
    this.searchResults = [];
    this.sortByOptions = [
      {
        name: 'Create Date',
        sort: 'createDate'
      },
      {
        name: 'A-Z',
        sort: 'name'
      },
      {
        name: 'Z-A',
        sort: '-name'
      }
    ];

    this.sortBy = this.sortByOptions[0];

    this.entityFiltersList = [
      {
        name: 'Datasets',
        isActive: true,
        isHover: false,
        filter: 'Dataset',
        count: 0
      },
      {
        name: 'Streams',
        isActive: true,
        isHover: false,
        filter: 'Stream',
        count: 0
      },
      {
        name: 'Stream Views',
        isActive: true,
        isHover: false,
        filter: 'Stream View',
        count: 0
      }
    ];

    this.metadataFiltersList = [
      {
        name: 'Name',
        isActive: false,
        isHover: false
      },
      {
        name: 'Description',
        isActive: false,
        isHover: false
      },
      {
        name: 'User Tags',
        isActive: false,
        isHover: false
      },
      {
        name: 'System Tags',
        isActive: false,
        isHover: false
      },
      {
        name: 'User Properties',
        isActive: false,
        isHover: false
      },
      {
        name: 'System Properties',
        isActive: false,
        isHover: false
      },
      {
        name: 'Schema',
        isActive: false,
        isHover: false
      }
    ];


    this.fetchResults();
  }

  /* Can be updated to use single query once backend supports it */
  fetchResults () {
    this.loading = true;

    let paramsBase = {
      namespace: this.$state.params.namespace,
      query: this.$state.params.searchQuery,
      scope: this.$scope
    };
    let datasetParams = angular.extend({target: 'dataset'}, paramsBase);
    let streamParams = angular.extend({target: 'stream'}, paramsBase);
    let viewParams = angular.extend({target: 'view'}, paramsBase);

    this.$q.all([
      this.myTrackerApi.search(datasetParams).$promise,
      this.myTrackerApi.search(streamParams).$promise,
      this.myTrackerApi.search(viewParams).$promise
    ]).then( (res) => {
      this.fullResults = this.fullResults.concat(res[0], res[1], res[2]);
      this.fullResults = this.fullResults.map(this.parseResult.bind(this));
      this.searchResults = angular.copy(this.fullResults);
      this.loading = false;
    }, (err) => {
      console.log('error', err);
      this.loading = false;
    });

  }

  parseResult (entity) {
    let obj = {};
    if (entity.entityId.type === 'datasetinstance') {
      angular.extend(obj, {
        name: entity.entityId.id.instanceId,
        type: 'Dataset',
        icon: 'icon-datasets',
        description: 'This is some description while waiting for backend to add description to these entities. Meanwhile, you can read this nonsense.',
        createDate: 1456299781,
        queryFound: ['User Tags', 'Schema', 'System Tags']
      });
      this.entityFiltersList[0].count++;
    } else if (entity.entityId.type === 'stream') {
      angular.extend(obj, {
        name: entity.entityId.id.streamName,
        type: 'Stream',
        icon: 'icon-streams',
        description: 'This is some description while waiting for backend to add description to these entities. Meanwhile, you can read this nonsense.',
        createDate: 1456299781,
        queryFound: ['User Tags', 'Schema', 'System Tags']
      });
      this.entityFiltersList[1].count++;
    } else if (entity.entityId.type === 'view') {
      // THIS SECTION NEEDS TO BE UPDATED
      angular.extend(obj, {
        name: entity.entityId.id.streamName,
        type: 'Stream View',
        icon: 'icon-streams',
        description: 'This is some description while waiting for backend to add description to these entities. Meanwhile, you can read this nonsense.',
        createDate: 1456299781,
        queryFound: ['User Tags', 'Schema', 'System Tags']
      });
      this.entityFiltersList[2].count++;
    }

    return obj;
  }

  onlyFilter(event, filter, filterType) {
    event.preventDefault();

    let filterObj = [];
    if (filterType === 'ENTITIES') {
      filterObj = this.entityFiltersList;
    } else if (filterType === 'METADATA') {
      filterObj = this.metadataFiltersList;
    }

    angular.forEach(filterObj, (entity) => {
      entity.isActive = entity.name === filter.name ? true : false;
    });

    this.filterResults();
  }

  filterResults() {
    let filter = [];

    angular.forEach(this.entityFiltersList, (entity) => {
      if (entity.isActive) { filter.push(entity.filter); }
    });

    this.searchResults = this.fullResults.filter( (result) => { return filter.indexOf(result.type) > -1 ? true : false; });
  }

  showAll (filterType) {
    let filterArr = [];
    if (filterType === 'ENTITIES') {
      filterArr = this.entityFiltersList;
    } else if (filterType === 'METADATA') {
      filterArr = this.metadataFiltersList;
    }

    angular.forEach(filterArr, (filter) => {
      filter.isActive = true;
    });

    this.filterResults();
  }

  evaluateShowResultCount() {
    let lowerLimit = (this.currentPage - 1) * 10 + 1;
    let upperLimit = (this.currentPage - 1) * 10 + 10;

    upperLimit = upperLimit > this.searchResults.length ? this.searchResults.length : upperLimit;

    return this.searchResults.length === 0 ? '0' : lowerLimit + '-' + upperLimit;
  }
}

TrackerResultsController.$inject = ['$state', 'myTrackerApi', '$scope', '$q'];

angular.module(PKG.name + '.feature.tracker')
 .controller('TrackerResultsController', TrackerResultsController);
