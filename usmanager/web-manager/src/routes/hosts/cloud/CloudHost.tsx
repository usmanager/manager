/*
 * Copyright (c) 2020 usmanager
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

import {RouteComponentProps} from "react-router";
import IData from "../../../components/IData";
import BaseComponent from "../../../components/BaseComponent";
import Form, {ICustomButton, IFields, required} from "../../../components/form/Form";
import ListLoadingSpinner from "../../../components/list/ListLoadingSpinner";
import Error from "../../../components/errors/Error";
import Field from "../../../components/form/Field";
import Tabs, {Tab} from "../../../components/tabs/Tabs";
import MainLayout from "../../../views/mainLayout/MainLayout";
import {ReduxState} from "../../../reducers";
import {addCloudHostRule, loadCloudHosts} from "../../../actions";
import {connect} from "react-redux";
import React from "react";
import {deleteData, IReply, postData} from "../../../utils/api";
import GenericHostRuleList from "../GenericHostRuleList";
import CloudHostRuleList from "./CloudHostRuleList";
import UnsavedChanged from "../../../components/form/UnsavedChanges";
import {Schemas} from "../../../middleware/api";
import {normalize} from "normalizr";

export interface ICloudHost extends IData {
  instanceId: string;
  imageId: string;
  instanceType: string;
  state: string;
  publicDnsName: string;
  publicIpAddress: string;
  hostRules?: string[];
}

export interface IState {
  code: number,
  name: string
}

const emptyCloudHost = (): Partial<ICloudHost> => ({

});

const isNewHost = (cloudHostInstanceId: string) =>
  cloudHostInstanceId === 'new_instance';

interface StateToProps {
  isLoading: boolean;
  error?: string | null;
  cloudHost: Partial<ICloudHost>;
  formCloudHost?: Partial<ICloudHost>;
}

interface DispatchToProps {
  loadCloudHosts: (instanceId: string) => any;
  addCloudHostRule: (hostname: string, ruleName: string) => void;
}

interface MatchParams {
  instanceId: string;
}

type Props = StateToProps & DispatchToProps & RouteComponentProps<MatchParams>;

type State = {
  newRules: string[],
  cloudHost?: ICloudHost,
  formCloudHost?: ICloudHost,
  isLoading: boolean,
}

class CloudHost extends BaseComponent<Props, {}> {

  private mounted = false;

  state: State = {
    newRules: [],
    isLoading: false,
  };

  componentDidMount(): void {
    this.loadCloudHost();
    this.mounted = true;
  };

  componentWillUnmount(): void {
    this.mounted = false;
  }

  private loadCloudHost = () => {
    const cloudHostInstanceId = this.props.match.params.instanceId;
    if (cloudHostInstanceId && !isNewHost(cloudHostInstanceId)) {
      this.props.loadCloudHosts(cloudHostInstanceId);
    }
  };

  private saveEntities = (cloudHost: ICloudHost) => {
    this.saveCloudHostRules(cloudHost.instanceId);
  };

  private onPostSuccess = (reply: IReply<ICloudHost>): void => {
    const cloudHost = reply.data;
    super.toast(`Cloud host <b>${cloudHost.instanceId}</b> has now started`);
    if (this.mounted) {
      this.updateCloudHost(cloudHost);
      this.props.history.replace(cloudHost.instanceId);
    }
    this.saveEntities(cloudHost);
  };

  private onPostFailure = (reason: string): void =>
    super.toast(`Unable to start a new cloud instance`, 10000, reason, true);

  private addCloudHostRule = (rule: string): void => {
    this.setState({
      newRules: this.state.newRules.concat(rule)
    });
  };

  private removeCloudHostRules = (rules: string[]): void => {
    this.setState({
      newRules: this.state.newRules.filter(rule => !rules.includes(rule))
    });
  };

  private saveCloudHostRules = (instanceId: string): void => {
    const {newRules} = this.state;
    if (newRules.length) {
      postData(`hosts/cloud/${instanceId}/rules`, newRules,
        () => this.onSaveRulesSuccess(instanceId),
        (reason) => this.onSaveRulesFailure(instanceId, reason));
    }
  };

  private onSaveRulesSuccess = (instanceId: string): void => {
    if (!isNewHost(this.props.match.params.instanceId)) {
      this.state.newRules.forEach(rule => this.props.addCloudHostRule(instanceId, rule));
    }
    if (this.mounted) {
      this.setState({ newRules: [] });
    }
  };

  private onSaveRulesFailure = (instanceId: string, reason: string): void =>
    super.toast(`Unable to save rules of host ${instanceId}`, 10000, reason, true);

  private shouldShowSaveButton = () =>
    !!this.state.newRules.length;

  private getFields = (cloudHost: Partial<ICloudHost>): IFields =>
    Object.entries(cloudHost).map(([key, _]) => {
      return {
        [key]: {
          id: key,
          label: key,
          validation: { rule: required }
        }
      };
    }).reduce((fields, field) => {
      for (let key in field) {
        fields[key] = field[key];
      }
      return fields;
    }, {});

  private startStopTerminateButtons = (): ICustomButton[] => {
    const state = this.getCloudHost().state;
    const buttons: ICustomButton[] = [];
    if (state?.includes('stopped')) {
      buttons.push({text: 'Start', onClick: this.startCloudHost});
    }
    if (state?.includes('running')) {
      buttons.push({text: 'Stop', onClick: this.stopCloudHost});
    }
    if (!state?.includes('terminated')) {
      buttons.push({text: 'Terminate', color: "red", onClick: this.terminateCloudHost,
        confirm: {id: 'terminate-cloudHost', message: `terminate instance ${(this.state.formCloudHost || this.props.formCloudHost)?.instanceId}`}});
    }
    return buttons;
  };

  private startCloudHost = () => {
    const cloudHost = this.getCloudHost();
    this.setState({isLoading: true});
    postData(`hosts/cloud/${cloudHost.instanceId}/state`, 'start',
      (reply: IReply<ICloudHost>) => this.onStartSuccess(reply.data),
      (reason) => this.onStartFailure(reason, cloudHost));
  };

  private onStartSuccess = (cloudHost: ICloudHost) => {
    super.toast(`Successfully started cloud host <b>${cloudHost.instanceId}</b>`, 15000);
    if (this.mounted) {
      this.updateCloudHost(cloudHost);
    }
  };

  private onStartFailure = (reason: string, cloudHost: Partial<ICloudHost>) => {
    super.toast(`Failed to start cloud host <b>${cloudHost.instanceId}</b>`, 10000, reason, true);
    if (this.mounted) {
      this.setState({isLoading: false});
    }
  };

  private stopCloudHost = () => {
    const cloudHost = this.getCloudHost();
    this.setState({isLoading: true});
    postData(`hosts/cloud/${cloudHost.instanceId}/state`, 'stop',
      (reply: IReply<ICloudHost>) => this.onStopSuccess(reply.data),
      (reason) => this.onStopFailure(reason, cloudHost));
  };

  private onStopSuccess = (cloudHost: ICloudHost) => {
    super.toast(`Successfully stopped cloud host <b>${cloudHost.instanceId}</b>`, 15000);
    if (this.mounted) {
      this.updateCloudHost(cloudHost);
    }
  };

  private onStopFailure = (reply: string, cloudHost: Partial<ICloudHost>) => {
    super.toast(`Failed to stop cloud host <b>${cloudHost.instanceId}</b>`, 10000, reply, true);
    if (this.mounted) {
      this.setState({isLoading: false});
    }
  };

  private terminateCloudHost = () => {
    const cloudHost = this.getCloudHost();
    this.setState({isLoading: true});
    deleteData(`hosts/cloud/${cloudHost.instanceId}`,
      () => this.onTerminateSuccess(cloudHost),
      (reason) => this.onTerminateFailure(reason, cloudHost));
  };

  private onTerminateSuccess = (cloudHost: Partial<ICloudHost>) => {
    super.toast(`Successfully terminated cloud host <b>${cloudHost.instanceId}</b>`, 15000);
    if (this.mounted) {
      this.props.history.push('/hosts/cloud');
    }
  };

  private onTerminateFailure = (reason: string, cloudHost: Partial<ICloudHost>) => {
    super.toast(`Failed to terminate cloud host <b>${cloudHost.instanceId}</b>`, 10000, reason, true);
    if (this.mounted) {
      this.setState({isLoading: false});
    }
  };

  private updateCloudHost = (cloudHost: ICloudHost) => {
    //TODO add rules from this.props.cloudHost
    console.log(this.props.cloudHost)
    cloudHost = Object.values(normalize(cloudHost, Schemas.CLOUD_HOST).entities.cloudHosts || {})[0];
    const formCloudHost = { ...cloudHost };
    removeFields(formCloudHost);
    this.setState({cloudHost: cloudHost, formCloudHost: formCloudHost, isLoading: false});
  };

  private getCloudHost = () =>
    this.state.cloudHost || this.props.cloudHost;

  private getFormCloudHost = () =>
    this.state.formCloudHost || this.props.formCloudHost;

  private cloudHost = () => {
    const {isLoading, error} = this.props;
    const cloudHost = this.getCloudHost();
    const formCloudHost = this.getFormCloudHost();
    // @ts-ignore
    const cloudHostKey: (keyof ICloudHost) = formCloudHost && Object.keys(formCloudHost)[0];
    return (
      <>
        {isLoading && <ListLoadingSpinner/>}
        {!isLoading && error && <Error message={error}/>}
        {!isLoading && !error && formCloudHost && (
          <Form id={cloudHostKey}
                fields={this.getFields(formCloudHost)}
                values={cloudHost}
                isNew={isNewHost(this.props.match.params.instanceId)}
                showSaveButton={this.shouldShowSaveButton()}
                post={{
                  url: 'hosts/cloud',
                  textButton: 'launch',
                  successCallback: this.onPostSuccess,
                  failureCallback: this.onPostFailure,
                  schema: Schemas.CLOUD_HOST}}
                customButtons={this.startStopTerminateButtons()}
                saveEntities={this.saveEntities}
                loading={this.state.isLoading}>
            {Object.keys(formCloudHost).map((key, index) =>
              <Field key={index}
                     id={key}
                     label={key}/>)}
          </Form>
        )}
      </>
    )
  };

  private entitiesList = (element: JSX.Element) => {
    const {isLoading, error} = this.props;
    const cloudHost = this.getCloudHost();
    if (isLoading) {
      return <ListLoadingSpinner/>;
    }
    if (error) {
      return <Error message={error}/>;
    }
    if (cloudHost) {
      return element;
    }
    return <></>;
  };

  private rules = (): JSX.Element =>
    this.entitiesList(<CloudHostRuleList host={this.getCloudHost()}
                                         unsavedRules={this.state.newRules}
                                         onAddHostRule={this.addCloudHostRule}
                                         onRemoveHostRules={this.removeCloudHostRules}/>);

  private genericRules = (): JSX.Element =>
    this.entitiesList(<GenericHostRuleList/>);

  private tabs: Tab[] = [
    {
      title: 'Cloud host',
      id: 'cloudHost',
      content: () => this.cloudHost()
    },
    {
      title: 'Rules',
      id: 'cloudRules',
      content: () => this.rules()
    },
    {
      title: 'Generic rules',
      id: 'genericEdgeRules',
      content: () => this.genericRules()
    },
  ];

  render() {
    return (
      <MainLayout>
        {/*{this.state.redirect && <Redirect to={`${this.state.redirect}`}/>}*/}
        {this.shouldShowSaveButton() && !isNewHost(this.props.match.params.instanceId) && <UnsavedChanged/>}
        <div className="container">
          <Tabs {...this.props} tabs={this.tabs}/>
        </div>
      </MainLayout>
    );
  }

}

function removeFields(cloudHost: Partial<ICloudHost>) {
  delete cloudHost["id"];
  delete cloudHost["hostRules"];
  if (!cloudHost.publicDnsName) {
    delete cloudHost["publicDnsName"];
  }
  if (!cloudHost.publicIpAddress) {
    delete cloudHost["publicIpAddress"];
  }
}

function mapStateToProps(state: ReduxState, props: Props): StateToProps {
  const isLoading = state.entities.hosts.cloud.isLoadingHosts;
  const error = state.entities.hosts.cloud.loadHostsError;
  const instanceId = props.match.params.instanceId;
  const cloudHost = isNewHost(instanceId) ? emptyCloudHost() : state.entities.hosts.cloud.data[instanceId];
  let formCloudHost;
  if (cloudHost) {
    formCloudHost = {...cloudHost};
    removeFields(formCloudHost);
  }
  return  {
    isLoading,
    error,
    cloudHost,
    formCloudHost
  }
}

const mapDispatchToProps: DispatchToProps = {
  loadCloudHosts,
  addCloudHostRule
};

export default connect(mapStateToProps, mapDispatchToProps)(CloudHost);