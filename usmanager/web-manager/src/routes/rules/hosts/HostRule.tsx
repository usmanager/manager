/*
 * Copyright (c) 2020 usmanager
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

import {ICondition, IDecision, IRule} from "../Rule";
import {RouteComponentProps} from "react-router";
import BaseComponent from "../../../components/BaseComponent";
import Form, {IFields, required, requiredAndNumberAndMin} from "../../../components/form/Form";
import LoadingSpinner from "../../../components/list/LoadingSpinner";
import Error from "../../../components/errors/Error";
import Field from "../../../components/form/Field";
import Tabs, {Tab} from "../../../components/tabs/Tabs";
import MainLayout from "../../../views/mainLayout/MainLayout";
import {ReduxState} from "../../../reducers";
import {connect} from "react-redux";
import React from "react";
import {addRuleHostCondition, loadDecisions, loadRulesHost, loadServices} from "../../../actions";
import {IService} from "../../services/Service";
import ServicePredictionList, {IPrediction} from "../../services/predictions/ServicePredictionList";
import {postData} from "../../../utils/api";
import {IAddServiceApp} from "../../services/apps/ServiceAppList";
import HostRuleConditionList from "./HostRuleConditionList";

export interface IHostRule extends IRule {
  hostname: string
}

const emptyHostRule = () => ({
  name: '',
  priority: 0,
  decision: undefined,
  hostname: '',
});

const isNewRule = (name: string) =>
  name === 'new_host_rule';

interface StateToProps {
  isLoading: boolean;
  error?: string | null;
  hostRule: Partial<IHostRule>;
  formHostRule?: Partial<IHostRule>,
  decisions: string[],
}

interface DispatchToProps {
  loadRulesHost: (name: string) => any;
  loadDecisions: (name?: string) => any;
  addRuleHostCondition: (ruleName: string, condition: string) => void;
}

interface MatchParams {
  name: string;
}

type Props = StateToProps & DispatchToProps & RouteComponentProps<MatchParams>;

type State = {
  newConditions: ICondition[],
}


class HostRule extends BaseComponent<Props, State> {

  state: State = {
    newConditions: [],
  };

  componentDidMount(): void {
    this.props.loadDecisions();
    const ruleName = this.props.match.params.name;
    if (ruleName && !isNewRule(ruleName)) {
      this.props.loadRulesHost(ruleName);
    }
  };

  private onPostSuccess = (reply: any, ruleName: string): void => {
    super.toast(`Host rule <b>${ruleName}</b> saved`);
  };

  private onPostFailure = (reason: string, ruleName: string): void =>
    super.toast(`Unable to save ${ruleName}`, 10000, reason, true);

  private onPutSuccess = (ruleName: string): void => {
    super.toast(`Changes to host rule <b>${ruleName}</b> are now saved`);
  };

  private onPutFailure = (reason: string, ruleName: string): void =>
    super.toast(`Unable to update ${ruleName}`, 10000, reason, true);

  private onDeleteSuccess = (ruleName: string): void => {
    super.toast(`Host rule <b>${ruleName}</b> successfully removed`);
    this.props.history.push(`/rules`)
  };

  private onDeleteFailure = (reason: string, ruleName: string): void =>
    super.toast(`Unable to delete ${ruleName}`, 10000, reason, true);

  private onAddRuleCondition = (condition: ICondition): void => {
    this.setState({
      newConditions: this.state.newConditions.concat(condition)
    });
  };

  private onRemoveRuleConditions = (conditions: string[]): void => {
    this.setState({
      newConditions: this.state.newConditions.filter(condition => !conditions.includes(condition.name))
    });
  };

  private saveRuleConditions = (ruleName: string): void => {
    const {newConditions} = this.state;
    if (newConditions.length) {
      postData(`rules/hosts/${ruleName}/conditions`, newConditions,
        () => this.onSaveConditionsSuccess(ruleName),
        (reason) => this.onSaveConditionsFailure(ruleName, reason));
    }
  };

  private onSaveConditionsSuccess = (ruleName: string): void => {
    if (!isNewRule(this.props.match.params.name)) {
      this.state.newConditions.forEach(condition =>
        this.props.addRuleHostCondition(ruleName, condition.name)
      );
    }
    this.setState({ newConditions: [] });
  };

  private onSaveConditionsFailure = (ruleName: string, reason: string): void =>
    super.toast(`Unable to save conditions of rule ${ruleName}`, 10000, reason, true);

  private getFields = (hostRule: Partial<IRule>): IFields =>
    Object.entries(hostRule).map(([key, value]) => {
      return {
        [key]: {
          id: [key],
          label: key,
          validation: key == 'hostname'
            ? undefined
            : (key == 'priority'
              ? { rule: requiredAndNumberAndMin, args: 0 }
              : { rule: required })
        }
      };
    }).reduce((fields, field) => {
      for (let key in field) {
        fields[key] = field[key];
      }
      return fields;
    }, {});

  private details = () => {
    const {isLoading, error, formHostRule, hostRule} = this.props;
    // @ts-ignore
    const ruleKey: (keyof IHostRule) = formHostRule && Object.keys(formHostRule)[0];
    return (
      <>
        {isLoading && <LoadingSpinner/>}
        {!isLoading && error && <Error message={error}/>}
        {!isLoading && !error && formHostRule && (
          <Form id={ruleKey}
                fields={this.getFields(formHostRule)}
                values={hostRule}
                isNew={isNewRule(this.props.match.params.name)}
                post={{url: 'rules', successCallback: this.onPostSuccess, failureCallback: this.onPostFailure}}
                put={{url: `rules/hosts/${hostRule[ruleKey]}`, successCallback: this.onPutSuccess, failureCallback: this.onPutFailure}}
                delete={{url: `rules/hosts/${hostRule[ruleKey]}`, successCallback: this.onDeleteSuccess, failureCallback: this.onDeleteFailure}}>
            {Object.keys(formHostRule).map((key, index) =>
              key === 'decision'
                ? <Field key={index}
                         id={[key, "name"]}
                         label={key}
                         type="dropdown"
                         dropdown={{defaultValue: "Choose decision", values: this.props.decisions}}/>
                : <Field key={index}
                         id={[key]}
                         label={key}/>
            )}
          </Form>
        )}
      </>
    )
  };

  private conditions = (): JSX.Element =>
    <HostRuleConditionList rule={this.props.hostRule}
                           newConditions={this.state.newConditions}
                           onAddRuleCondition={this.onAddRuleCondition}
                           onRemoveRuleConditions={this.onRemoveRuleConditions}/>;

  private tabs: Tab[] = [
    {
      title: 'Host rule',
      id: 'hostRule',
      content: () => this.details(),
    },
    {
      title: 'Conditions',
      id: 'hostRuleConditions',
      content: () => this.conditions(),
    }
  ];

  render() {
    return (
      <MainLayout>
        <div className="container">
          <Tabs {...this.props} tabs={this.tabs}/>
        </div>
      </MainLayout>
    );
  }

}

function mapStateToProps(state: ReduxState, props: Props): StateToProps {
  const isLoading = state.entities.rules.hosts.isLoading;
  const error = state.entities.rules.hosts.error;
  const name = props.match.params.name;
  const hostRule = isNewRule(name) ? emptyHostRule() : state.entities.rules.hosts.data[name];
  let formHostRule;
  if (hostRule) {
    formHostRule = { ...hostRule };
    delete formHostRule["id"];
    if (formHostRule["hostname"] == null) {
      delete formHostRule["hostname"];
      delete formHostRule["conditions"];
    }
  }
  const decisions = state.entities.decisions.data
    && Object.entries(state.entities.decisions.data)
             .filter(([_, value]) => value.componentType.name.toLowerCase() == 'host')
             .map(([key, value]) => key);
  return  {
    isLoading,
    error,
    hostRule,
    formHostRule,
    decisions
  }
}

const mapDispatchToProps: DispatchToProps = {
  loadRulesHost,
  loadDecisions,
  addRuleHostCondition
};

export default connect(mapStateToProps, mapDispatchToProps)(HostRule);
