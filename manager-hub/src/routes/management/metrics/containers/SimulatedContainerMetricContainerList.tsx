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

import React from "react";
import BaseComponent from "../../../../components/BaseComponent";
import ListItem from "../../../../components/list/ListItem";
import styles from "../../../../components/list/ListItem.module.css";
import ControlledList from "../../../../components/list/ControlledList";
import {ReduxState} from "../../../../reducers";
import {bindActionCreators} from "redux";
import {connect} from "react-redux";
import {Link} from "react-router-dom";
import {
    loadContainers,
    loadSimulatedContainerMetricContainers,
    removeSimulatedContainerMetricContainers,
} from "../../../../actions";
import {IContainer} from "../../containers/Container";
import {ISimulatedContainerMetric} from "./SimulatedContainerMetric";

interface StateToProps {
    isLoading: boolean;
    error?: string | null;
    containers: { [key: string]: IContainer },
    simulatedMetricContainers: string[];
}

interface DispatchToProps {
    loadContainers: () => void;
    loadSimulatedContainerMetricContainers: (name: string) => void;
    removeSimulatedContainerMetricContainers: (name: string, containers: string[]) => void;
}

interface SimulatedContainerMetricContainerListProps {
    isLoadingSimulatedContainerMetric: boolean;
    loadSimulatedContainerMetricError?: string | null;
    simulatedContainerMetric: ISimulatedContainerMetric | Partial<ISimulatedContainerMetric> | null;
    unsavedContainersIds: string[];
    unsavedContainers: string[];
    onAddContainer: (container: string) => void;
    onRemoveContainers: (container: string[]) => void;
}

type Props = StateToProps & DispatchToProps & SimulatedContainerMetricContainerListProps;

interface State {
    entitySaved: boolean;
}

class SimulatedContainerMetricContainerList extends BaseComponent<Props, State> {

    constructor(props: Props) {
        super(props);
        this.state = {entitySaved: !this.isNew()};
    }

    public componentDidMount(): void {
        this.props.loadContainers();
        this.loadEntities();
    }

    public componentDidUpdate(prevProps: Readonly<Props>, prevState: Readonly<State>, snapshot?: any): void {
        if (!prevProps.simulatedContainerMetric?.name && this.props.simulatedContainerMetric?.name) {
            this.setState({entitySaved: true});
        }
    }

    public render() {
        const isNew = this.isNew();
        return <ControlledList
            isLoading={!isNew ? this.props.isLoadingSimulatedContainerMetric || this.props.isLoading : undefined}
            error={this.props.loadSimulatedContainerMetricError || (isNew ? undefined : this.props.error)}
            emptyMessage={`Sem contentores associados`}
            data={this.props.simulatedMetricContainers}
            dropdown={{
                id: 'containers',
                title: 'Selecionar o contentor',
                empty: 'Não há contentores disponíveis',
                data: this.getSelectableContainers()
            }}
            show={this.container}
            onAdd={this.onAdd}
            onRemove={this.onRemove}
            onDelete={{
                url: `simulated-metrics/containers/${this.props.simulatedContainerMetric?.name}/containers`,
                successCallback: this.onDeleteSuccess,
                failureCallback: this.onDeleteFailure
            }}
            entitySaved={this.state.entitySaved}
            invalidate={this.invalidate}/>;
    }

    private loadEntities = () => {
        if (this.props.simulatedContainerMetric?.name) {
            const {name} = this.props.simulatedContainerMetric;
            this.props.loadSimulatedContainerMetricContainers(name);
        }
    };

    private isNew = () =>
        this.props.simulatedContainerMetric?.name === undefined;

    private container = (index: number, container: string, separate: boolean, checked: boolean,
                         handleCheckbox: (event: React.ChangeEvent<HTMLInputElement>) => void): JSX.Element => {
        const isNew = this.isNew();
        const unsaved = this.props.unsavedContainersIds.includes(container);
        return (
            <ListItem key={index} separate={separate}>
                <div className={`${styles.linkedItemContent}`}>
                    <label>
                        <input id={container}
                               type="checkbox"
                               onChange={handleCheckbox}
                               checked={checked}/>
                        <span id={'checkbox'}>
                            <div className={!isNew && unsaved ? styles.unsavedItem : undefined}>
                                {container}
                            </div>
                        </span>
                    </label>
                </div>
                {!isNew && (
                    <Link to={`/contentores/${container}`}
                          className={`${styles.link}`}>
                        <i className={`link-icon material-icons right`}>link</i>
                    </Link>
                )}
            </ListItem>
        );
    };

    private onAdd = (container: string): void =>
        this.props.onAddContainer(container);

    private onRemove = (container: string[]) =>
        this.props.onRemoveContainers(container);

    private onDeleteSuccess = (container: string[]): void => {
        if (this.props.simulatedContainerMetric?.name) {
            const {name} = this.props.simulatedContainerMetric;
            this.props.removeSimulatedContainerMetricContainers(name, container);
        }
    };

    private onDeleteFailure = (reason: string): void =>
        super.toast(`Não foi possível remover o contentor`, 10000, reason, true);

    private getSelectableContainers = () => {
        const {containers, simulatedMetricContainers, unsavedContainersIds} = this.props;
        return Object.entries(containers).filter(([containerId, _]) => !simulatedMetricContainers.includes(containerId)
            && !unsavedContainersIds.includes(containerId))
            .map(([_, container]) => container.name.replace('/', '') + " - " + container.id);
    };

    private invalidate = (data: string): string | undefined =>
        this.props.unsavedContainers.find(container => container.split(" - ")[1] === data)

}

function mapStateToProps(state: ReduxState, ownProps: SimulatedContainerMetricContainerListProps): StateToProps {
    const name = ownProps.simulatedContainerMetric?.name;
    const simulatedMetric = name && state.entities.simulatedMetrics.containers.data[name];
    const simulatedMetricContainers = simulatedMetric && simulatedMetric.containers;
    return {
        isLoading: state.entities.simulatedMetrics.containers.isLoadingContainers,
        error: state.entities.simulatedMetrics.containers.loadContainersError,
        containers: state.entities.containers.data,
        simulatedMetricContainers: simulatedMetricContainers || [],
    }
}

const mapDispatchToProps = (dispatch: any): DispatchToProps =>
    bindActionCreators({
        loadContainers,
        loadSimulatedContainerMetricContainers,
        removeSimulatedContainerMetricContainers,
    }, dispatch);

export default connect(mapStateToProps, mapDispatchToProps)(SimulatedContainerMetricContainerList);
