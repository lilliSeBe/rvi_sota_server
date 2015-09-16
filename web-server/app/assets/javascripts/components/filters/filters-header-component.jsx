define(function(require) {
  var React = require('react'),
      Store = require('../../stores/filters'),
      AddFilterComponent = require('./add-filter-component');

  var FiltersHeaderComponent = React.createClass({
    render: function() {
      return (
      <div>
        <div className="row">
          <div className="col-md-12">
            <h1>
              Filters
            </h1>
          </div>
        </div>
        <div className="row">
          <div className="col-md-8">
            <p>
            </p>
          </div>
        </div>
        <AddFilterComponent Store={Store}/>
      </div>
    );}
  });

  return FiltersHeaderComponent;

});
