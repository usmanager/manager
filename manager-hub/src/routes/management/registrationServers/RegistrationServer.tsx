/*
 * MIT License
 *
 * Copyright (c) 2020 manager
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

import BaseComponent from "../../../components/BaseComponent";
import React from "react";
import {RouteComponentProps} from "react-router";
import Form, {IFields, requiredAndNumberAndMin, requiredAndTrimmed} from "../../../components/form/Form";
import LoadingSpinner from "../../../components/list/LoadingSpinner";
import {Error} from "../../../components/errors/Error";
import Field, {getTypeFromValue} from "../../../components/form/Field";
import Tabs, {Tab} from "../../../components/tabs/Tabs";
import MainLayout from "../../../views/mainLayout/MainLayout";
import {ReduxState} from "../../../reducers";
import {addRegistrationServers, loadNodes, loadRegions, loadRegistrationServers,} from "../../../actions";
import {connect} from "react-redux";
import {IRegion} from "../regions/Region";
import {IReply} from "../../../utils/api";
import {isNew} from "../../../utils/router";
import {IContainer} from "../containers/Container";
import {normalize} from "normalizr";
import {Schemas} from "../../../middleware/api";
import {IHostAddress} from "../hosts/Hosts";
import {INode} from "../nodes/Node";
import {IDatabaseData} from "../../../components/IData";

export interface IRegistrationServer extends IDatabaseData {
    container: IContainer,
    region: IRegion,
}

interface INewRegistrationServerRegion {
    regions: string[] | undefined
}

interface INewRegistrationServerHost {
    hostAddress: IHostAddress | undefined
}

const buildNewRegistrationServerRegion = (): INewRegistrationServerRegion => ({
    regions: undefined
});

const buildNewRegistrationServerHost = (): INewRegistrationServerHost => ({
    hostAddress: undefined
});

interface StateToProps {
    isLoading: boolean;
    error?: string | null;
    newRegistrationServerHost?: INewRegistrationServerHost;
    newRegistrationServerRegion?: INewRegistrationServerRegion;
    registrationServer?: IRegistrationServer;
    formRegistrationServer?: Partial<IRegistrationServer>;
    regions: { [key: string]: IRegion };
    nodes: { [key: string]: INode };
    registrationServers: { [key: string]: IRegistrationServer };
}

interface DispatchToProps {
    loadRegistrationServers: (id?: string) => void;
    addRegistrationServers: (registrationServers: IRegistrationServer[]) => void;
    loadRegions: () => void;
    loadNodes: () => void;
}

interface MatchParams {
    id: string;
}

interface LocationState {
    data: IRegistrationServer,
    selected: 'registration-server';
}

type Props = StateToProps & DispatchToProps & RouteComponentProps<MatchParams, {}, LocationState>;

interface State {
    registrationServer?: IRegistrationServer,
    formRegistrationServer?: IRegistrationServer,
    currentForm: 'Por regiões' | 'Num endereço',
}

class RegistrationServer extends BaseComponent<Props, State> {

    state: State = {
        currentForm: 'Por regiões'
    };
    private mounted = false;

    public componentDidMount(): void {
        if (isNew(this.props.location.search)) {
            this.props.loadRegistrationServers();
        } else {
            const workerManagerId = this.props.match.params.id;
            this.props.loadRegistrationServers(workerManagerId);
        }
        this.props.loadRegions();
        this.props.loadNodes();
        this.mounted = true;
    }

    public componentWillUnmount(): void {
        this.mounted = false;
    }

    public render() {
        return (
            <MainLayout>
                <div className="container">
                    <Tabs {...this.props} tabs={this.tabs()}/>
                </div>
            </MainLayout>
        );
    }

    private getRegistrationServer = () =>
        this.props.registrationServer || this.state.registrationServer;

    private getFormRegistrationServer = () =>
        this.props.formRegistrationServer || this.state.formRegistrationServer;

    private isNew = () =>
        isNew(this.props.location.search);

    private onPostSuccess = (reply: IReply<IRegistrationServer[]>): void => {
        let registrationServers = reply.data;
        if (registrationServers.length === 1) {
            const registrationServer = registrationServers[0];
            super.toast(`<span class="green-text">O servidor de registo foi iniciado no contentor ${this.mounted ? `<b>${registrationServer.container.id}</b>` : `<a href='/servidores de registo/${registrationServer.id}'><b>${registrationServer.container.id}</b></a>`}</span>`);
            if (this.mounted) {
                this.updateRegistrationServer(registrationServer);
                this.props.history.replace(registrationServer.id.toString())
            }
        } else {
            registrationServers = registrationServers.reverse();
            super.toast(`<span class="green-text">Foram iniciados ${registrationServers.length} servidores de registo novos:<br/><b>${registrationServers.map(registrationServer => `Contentor ${registrationServer.container.id} => Host ${registrationServer.container.publicIpAddress}`).join('<br/>')}</b></span>`);
            if (this.mounted) {
                this.props.history.push('/servidores de registo');
            }
        }
        this.props.addRegistrationServers(registrationServers);
    };

    private onPostFailure = (reason: string): void =>
        super.toast(`Não foi possível lançar o servidor de registo`, 10000, reason, true);

    private onDeleteSuccess = (registrationServer: IRegistrationServer): void => {
        super.toast(`<span class="green-text">O servidor de registo <b>${registrationServer.id}</b> foi parado com sucesso</span>`);
        if (this.mounted) {
            this.props.history.push(`/servidores de registo`)
        }
    };

    private onDeleteFailure = (reason: string, registrationServer: IRegistrationServer): void =>
        super.toast(`Não foi possível parar o servidor de registo ${this.mounted ? `<b>${registrationServer.id}</b>` : `<a href='/servidores de registo/${registrationServer.id}'><b>${registrationServer.id}</b></a>`}`, 10000, reason, true);

    private updateRegistrationServer = (registrationServer: IRegistrationServer) => {
        registrationServer = Object.values(normalize(registrationServer, Schemas.REGISTRATION_SERVER).entities.registrationServers || {})[0];
        const formRegistrationServer = {...registrationServer};
        this.setState({registrationServer: registrationServer, formRegistrationServer: formRegistrationServer});
    };

    private getFields = (registrationServer: INewRegistrationServerRegion | INewRegistrationServerHost | IRegistrationServer): IFields =>
        Object.entries(registrationServer).map(([key, value]) => {
            return {
                [key]: {
                    id: key,
                    label: key,
                    validation: getTypeFromValue(value) === 'number'
                        ? {rule: requiredAndNumberAndMin, args: 0}
                        : {rule: requiredAndTrimmed}
                }
            };
        }).reduce((fields, field) => {
            for (let key in field) {
                fields[key] = field[key];
            }
            return fields;
        }, {});

    private getSelectableHosts = (): Partial<IHostAddress>[] =>
        Object.entries(this.props.nodes)
            .filter(([_, node]) => node.state === 'ready' && node.labels['place'] === 'CLOUD')
            .map(([_, node]) =>
                ({
                    username: node.labels['username'],
                    publicIpAddress: node.publicIpAddress,
                    privateIpAddress: node.labels['privateIpAddress'],
                    coordinates: node.labels['coordinates'] ? JSON.parse(node.labels['coordinates']) : undefined,
                }))

    private hostAddressesDropdown = (hostAddress: Partial<IHostAddress>): string =>
        hostAddress.publicIpAddress + (hostAddress.privateIpAddress ? ("/" + hostAddress.privateIpAddress) : '') + " - " + hostAddress.coordinates?.label;

    private regionOption = (region: IRegion) =>
        region.region;

    private getSelectableRegions = (): string[] => {
        const deployedRegions = Object.values(this.props.registrationServers).map(registrationServer => registrationServer.region.region);
        return Object.keys(this.props.regions).filter(region => !deployedRegions.includes(region));
    }

    private containerIdField = (container: IContainer) =>
        container.id.toString();

    private containerPublicIpAddressField = (container: IContainer) =>
        container.publicIpAddress;

    private formFields = (isNew: boolean, formRegistrationServer?: Partial<IRegistrationServer>) => {
        const {currentForm} = this.state;
        return (
            isNew ?
                currentForm === 'Por regiões'
                    ?
                    <Field key={'regions'}
                           id={'regions'}
                           label={'regions'}
                           type={'list'}
                           value={this.getSelectableRegions()}/>
                    :
                    <>
                        <Field<Partial<IHostAddress>> key={'hostAddress'}
                                                      id={'hostAddress'}
                                                      label={'hostAddress'}
                                                      type="dropdown"
                                                      dropdown={{
                                                          defaultValue: "Selecionar o endereço",
                                                          values: this.getSelectableHosts(),
                                                          optionToString: this.hostAddressesDropdown,
                                                          emptyMessage: 'Náo há hosts disponíveis'
                                                      }}/>
                    </>
                : formRegistrationServer && Object.entries(formRegistrationServer).map((([key, value], index) =>
                key === 'container'
                    ? <>
                        <Field<IContainer> key={index}
                                           id={key}
                                           label={key + " id"}
                                           valueToString={this.containerIdField}
                                           icon={{linkedTo: '/contentores/' + (formRegistrationServer as Partial<IRegistrationServer>).container?.id}}/>
                        <Field<IContainer>
                            key={5000}
                            id={key}
                            label={"host"}
                            valueToString={this.containerPublicIpAddressField}/>
                    </>
                    : key === 'region'
                    ? <Field<IRegion> key={index}
                                      id={key}
                                      type="dropdown"
                                      label={key}
                                      valueToString={this.regionOption}
                                      dropdown={{
                                          defaultValue: "Selecionar a região",
                                          emptyMessage: "Não há regiões disponíveis",
                                          values: [(formRegistrationServer as IRegistrationServer).region],
                                          optionToString: this.regionOption
                                      }}/>
                    : <Field key={index}
                             id={key}
                             label={key}/>))
        );
    };

    private switchForm = (formId: 'Por regiões' | 'Num endereço') =>
        this.setState({currentForm: formId});

    private onPost = (registrationServers: IRegistrationServer[]) => {
        if (registrationServers.length == 1) {
            return registrationServers[0];
        }
        return registrationServers;
    }

    private registrationServer = () => {
        const {isLoading, error, newRegistrationServerRegion, newRegistrationServerHost} = this.props;
        const {currentForm} = this.state;
        const isNewRegistrationServer = this.isNew();
        const registrationServer = isNewRegistrationServer ? (currentForm === 'Por regiões' ? newRegistrationServerRegion : newRegistrationServerHost) : this.getRegistrationServer();
        const formRegistrationServer = this.getFormRegistrationServer();
        // @ts-ignore
        const registrationServerKey: (keyof IRegistrationServer) = formRegistrationServer && Object.keys(formRegistrationServer)[0];
        return (
            <>
                {isLoading && <LoadingSpinner/>}
                {!isLoading && error && <Error message={error}/>}
                {!isLoading && !error && registrationServer && (
                    /*@ts-ignore*/
                    <Form id={registrationServerKey}
                          fields={this.getFields(registrationServer)}
                          values={registrationServer}
                          isNew={isNew(this.props.location.search)}
                          showSaveButton={false}
                          post={{
                              textButton: 'Executar',
                              url: 'registration-servers',
                              successCallback: this.onPostSuccess,
                              failureCallback: this.onPostFailure,
                              result: this.onPost,
                          }}
                          delete={{
                              textButton: 'Parar',
                              url: `registration-servers/${(registrationServer as IRegistrationServer).id}`,
                              successCallback: this.onDeleteSuccess,
                              failureCallback: this.onDeleteFailure
                          }}
                        // registration servers must be launched on cloud hosts and on a specific aws region
                        // to be able to assign it an elastic ip
                        /*switchDropdown={isNewRegistrationServer ? {
                            options: currentForm === 'Por regiões' ? ['Num endereço'] : ['Por regiões'],
                            onSwitch: this.switchForm
                        } : undefined}*/>
                        {this.formFields(isNewRegistrationServer, formRegistrationServer)}
                    </Form>
                )}
            </>
        )
    };

    private tabs = (): Tab[] => [
        {
            title: 'Servidor de registo',
            id: 'registrationServer',
            content: () => this.registrationServer(),
            active: this.props.location.state?.selected === 'registration-server'
        },
    ];

}

function mapStateToProps(state: ReduxState, props: Props): StateToProps {
    const isLoading = state.entities.registrationServers.isLoadingRegistrationServers;
    const error = state.entities.registrationServers.loadRegistrationServersError;
    const id = props.match.params.id;
    const newRegistrationServer = isNew(props.location.search);
    const newRegistrationServerRegion = newRegistrationServer ? buildNewRegistrationServerRegion() : undefined;
    const newRegistrationServerHost = newRegistrationServer ? buildNewRegistrationServerHost() : undefined;
    const registrationServer = !newRegistrationServer ? state.entities.registrationServers.data[id] : undefined;
    let formRegistrationServer;
    if (registrationServer) {
        formRegistrationServer = {...registrationServer};
    }
    const regions = state.entities.regions.data;
    const nodes = state.entities.nodes.data;
    const registrationServers = state.entities.registrationServers.data;
    return {
        isLoading,
        error,
        newRegistrationServerRegion,
        newRegistrationServerHost,
        registrationServer,
        formRegistrationServer,
        regions,
        nodes,
        registrationServers
    }
}

const mapDispatchToProps: DispatchToProps = {
    loadRegistrationServers,
    addRegistrationServers,
    loadRegions,
    loadNodes,
};

export default connect(mapStateToProps, mapDispatchToProps)(RegistrationServer);