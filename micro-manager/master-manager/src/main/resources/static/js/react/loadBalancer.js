/*
 * MIT License
 *
 * Copyright (c) 2020 micro-manager
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

let $ = require('jquery');
let React = require('react');
let Redirect = require('react-router-dom').Redirect;
let Component = React.Component;
import Utils from './utils';
import {MainLayout} from './globalComponents';

export class LoadBalancerPage extends Component {

    constructor(props) {
        super(props);
        this.state = {services: [], chosenRegions: [], availableRegions: [], formSubmit: false, loading: false};
    }

    componentDidMount() {
        this.loadRegions();
        this.loadServices();
    }

    componentDidUpdate() {
        let elems = document.querySelectorAll('select');
        M.FormSelect.init(elems);
    }

    loadServices = () => {
        this.setState({ loading: true });  
        let self = this;
        Utils.ajaxGet('/services',
            function (data) {
                let frontendServices = [];
                for (let index = 0; index < data.length; index++) {
                    if (data[index].serviceType === "frontend") {
                        frontendServices.push(data[index]);
                    }                    
                }
                self.setState({services: frontendServices, loading: false});
            });
    };

    loadRegions = () => {
        this.setState({ loading: true });  
        let self = this;
        Utils.ajaxGet('/regions',
            function (data) {             
                self.setState({availableRegions: data, loading: false});
            });
    };

    addRegion = (regionId, event) => {
        let self = this;
        function getIndex(regionId, regions) {
            let i;
            for (i = 0; i < regions.length; i++) {
                if (regions[i].id === regionId) {
                    return i;
                }
            }
        }
        let newAvailableRegions = self.state.availableRegions;
        let index = getIndex(regionId, newAvailableRegions);
        newAvailableRegions.splice(index, 1);
        this.setState({ loading: true });          
        Utils.ajaxGet('/regions/' + regionId,
            function (data) {
                let newChosenRegions = self.state.chosenRegions;
                newChosenRegions.push(data);
                self.setState({availableRegions: newAvailableRegions, chosenRegions: newChosenRegions, loading: false});
            });
    };

    onRemoveRegion = (regionId, event) => {
        let self = this;
        function getIndex(regionId, regions) {
            let i;
            for (i = 0; i < regions.length; i++){
                if (regions[i].id === regionId){
                    return i;
                }
            }
        }

        let newChosenRegions = self.state.chosenRegions;
        let index = getIndex(regionId, newChosenRegions);
        newChosenRegions.splice(index, 1);
        this.setState({ loading: true });          
        Utils.ajaxGet('/regions/' + regionId,
            function (data) {
                let newAvailableRegions = self.state.availableRegions;
                newAvailableRegions.push(data);
                self.setState({availableRegions: newAvailableRegions, chosenRegions: newChosenRegions, loading: false});
            });
        
    };

    renderServicesSelect = () => {
        let servicesNodes;
        if (this.state.services) {
            servicesNodes = this.state.services.map(function (service) {
                return (
                    <option key={service.id} value={service.serviceName}>{service.serviceName}</option>
                );
            });
            return servicesNodes;
        }
    };

    onSubmitForm = (event) => {
        event.preventDefault();
        let self = this;
        let formAction = '/containers/loadBalancer';
        let service = $('#service').val();
        let dataToSend = {
            serviceName : service,
            regions : self.state.chosenRegions
        };
        let formData = JSON.stringify(dataToSend);
        Utils.formSubmit(formAction, 'POST', formData, function (data) {
            self.setState({formSubmit: true});
            let hosts = data.toString();
            M.toast({html: "<div>Load balancers successfully launched!</br>Hosts: " + hosts + "</div>"});
        });
    };

    renderChosenRegions = () => {
        let regionsNodes;
        let self = this;
        let style = {marginTop: '-4px'};
        if (this.state.chosenRegions) {
            regionsNodes = this.state.chosenRegions.map(function (region) {
                return (
                    <li key={region.id} className="collection-item">
                        <div>
                            {region.regionName + " (" + region.regionDescription + ")"}
                            <a style={style} className="secondary-content btn-floating btn-small waves-effect waves-light" onClick={(e) => self.onRemoveRegion(region.id, e)}>
                                <i className="material-icons">clear</i>
                            </a>                            
                        </div>
                    </li>                   
                );
            });
        }
        return regionsNodes;
    };

    renderAvailableRegions = () => {
        let regionsNodes;
        let style = {marginTop: '-4px'};
        let self = this;
        if (this.state.availableRegions) {
            regionsNodes = this.state.availableRegions.map(function (region) {
                return (
                    <li key={region.id} className="collection-item">
                        <div>
                        {region.regionName + " (" + region.regionDescription + ")"}
                            <a style={style} className="secondary-content btn-floating btn-small waves-effect waves-light" onClick={(e) => self.addRegion(region.id, e)}>
                                <i className="material-icons">add</i>
                            </a>                            
                        </div>
                    </li>
                );                              
            });
        }
        return (
            <ul className="collection">                    
                {regionsNodes}
            </ul>
        )
    };

    renderLoadBalancerPageComponents = () => {
        return (
            <div>
                <div className="input-field col s12">
                    <select defaultValue="" name="service" id="service">
                        <option value="" disabled="disabled">Choose service</option>
                        {this.renderServicesSelect()}
                    </select>
                    <label htmlFor="service">Service</label>
                </div>
                <h5>Chosen Regions</h5>
                <ul className="collection">
                    {this.renderChosenRegions()}
                </ul>
                <form id='launchLoadBalancerForm' onSubmit={this.onSubmitForm}>                
                    <button disabled={this.state.chosenRegions.length === 0} className="btn waves-effect waves-light" type="submit" name="action">
                        Launch load balancers
                        <i className="material-icons right">send</i>
                    </button>
                </form>
                <br/>
                <h5>Available regions</h5>
                {this.renderAvailableRegions()}
            </div>
        )
    };

    render() {
        if(this.state.formSubmit){
            return <Redirect to='/ui/home' />;
        }    
        return (
            <MainLayout title='Launch load balancers' breadcrumbs={this.state.breadcrumbs}>
                {this.renderLoadBalancerPageComponents()}
            </MainLayout>            
        );
    }
}