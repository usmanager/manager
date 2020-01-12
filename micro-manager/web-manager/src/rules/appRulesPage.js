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

import React from 'react';
import M from 'materialize-css';
import Utils from '../utils';
import { MainLayout } from '../sharedComponents/mainLayout';

export class AppRulesPage extends React.Component {
  constructor (props) {
    super(props);
    let thisAppId = 0;
    if (props.match.params.appId) {
      thisAppId = props.match.params.appId;
    }
    const thisBreadcrumbs = [{ link: '/ui/rules/apps', title: 'Apps rules' }];
    this.state = {
      breadcrumbs: thisBreadcrumbs,
      appId: thisAppId,
      app: {},
      rules: [],
      allRules: [],
      loadedRules: false,
      loading: false
    };
  }

  componentDidMount () {
    this.loadApp();
    this.loadAppRules();
    this.loadAllRules();
  }

  componentDidUpdate () {
    M.updateTextFields();
  }

  loadApp () {
    const self = this;
    Utils.ajaxGet('/apps/' + self.state.appId,
      function (data) {
        self.setState({ app: data, loading: false });
      });
  };

  loadAppRules () {
    this.setState({ loadedRules: false, loading: true });
    const self = this;
    Utils.ajaxGet('/apps/' + self.state.appId + '/rules',
      function (data) {
        self.setState({ rules: data, loadedRules: true, loading: false });
      });
  };

  loadAllRules () {
    this.setState({ loading: true });
    const self = this;
    Utils.ajaxGet('/rules/container',
      function (data) {
        self.setState({ allRules: data, loading: false });
      });
  };

  onRemoveRule (ruleId, event) {
    const formAction = '/apps/' + this.state.appId + '/rules';
    const self = this;
    const data = {
      appId: Number(self.state.appId),
      ruleId: Number(ruleId)
    };
    Utils.formSubmit(formAction, 'DELETE', JSON.stringify(data), function (data) {
      M.toast({ html: '<div>Rule successfully deleted from app rules!</div>' });
      self.loadAppRules();
    });
  };

  addRule (ruleId, event) {
    const formAction = '/apps/' + this.state.appId + '/rules';
    const self = this;
    const data = {
      appId: Number(self.state.appId),
      ruleId: Number(ruleId)
    };
    Utils.formSubmit(formAction, 'POST', JSON.stringify(data), function (data) {
      M.toast({ html: '<div>Rule successfully added to app rules!</div>' });
      self.loadAppRules();
    });
  };

  renderRules () {
    let rulesNodes;
    const self = this;
    const style = { marginTop: '-4px' };
    if (this.state.rules) {
      rulesNodes = this.state.rules.map(function (appRule) {
        return (
          <li key={appRule.rule.id} className="collection-item">
            <div>
              {appRule.rule.ruleName}
              <a style={style} className="secondary-content btn-floating btn-small waves-effect waves-light"
                onClick={(e) => self.onRemoveRule(appRule.rule.id, e)}>
                <i className="material-icons">clear</i>
              </a>
            </div>
          </li>
        );
      });
    }
    return rulesNodes;
  };

  renderAddRules () {
    let ruleNodes;
    const style = { marginTop: '-4px' };
    const self = this;

    function canAddRule (ruleId) {
      for (let i = 0; i < self.state.rules.length; i++) {
        if (self.state.rules[i].rule.id === ruleId) {
          return false;
        }
      }
      return true;
    }

    if (this.state.allRules && this.state.loadedRules) {
      ruleNodes = this.state.allRules.map(function (rule) {
        if (canAddRule(rule.id)) {
          return (
            <li key={rule.id} className="collection-item">
              <div>
                {rule.ruleName}
                <a style={style} className="secondary-content btn-floating btn-small waves-effect waves-light"
                  onClick={(e) => self.addRule(rule.id, e)}>
                  <i className="material-icons">add</i>
                </a>
              </div>
            </li>
          );
        }
      });
    }
    return (
      <ul className="collection">
        {ruleNodes}
      </ul>
    );
  };

  render () {
    return (
      <MainLayout title='App rules detail' breadcrumbs={this.state.breadcrumbs}>
        <div className='row'>
          <div className="input-field col s12">
            <input disabled={true} value={this.state.app.appName} name="appName" id="appName" type="text"/>
            <label htmlFor="appName">App name</label>
          </div>
          <div>
            <h5>Rules</h5>
            <ul className="collection">
              {this.renderRules()}
            </ul>
            <br/>
            <h5>Add rules</h5>
            {this.renderAddRules()}
          </div>
        </div>
      </MainLayout>
    );
  }
}
