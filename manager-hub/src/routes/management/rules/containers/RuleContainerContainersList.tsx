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

import BaseComponent from "../../../../components/BaseComponent";
import ListItem from "../../../../components/list/ListItem";
import styles from "../../../../components/list/ListItem.module.css";
import React from "react";
import ControlledList from "../../../../components/list/ControlledList";
import {ReduxState} from "../../../../reducers";
import {bindActionCreators} from "redux";
import {loadContainers, loadRuleContainers, removeRuleContainers,} from "../../../../actions";
import {connect} from "react-redux";
import {Link} from "react-router-dom";
import {IContainer} from "../../containers/Container";
import {IRuleContainer} from "./RuleContainer";

interface StateToProps {
    isLoading: boolean;
    error?: string | null;
    ruleContainers: string[];
    containers: { [key: string]: IContainer };
}

interface DispatchToProps {
    loadContainers: () => void;
    loadRuleContainers: (ruleName: string) => void;
    removeRuleContainers: (ruleName: string, containers: string[]) => void;
}

interface ContainerRuleContainersListProps {
    isLoadingRuleContainer: boolean;
    loadRuleContainerError?: string | null;
    ruleContainer: IRuleContainer | Partial<IRuleContainer> | null;
    unsavedContainersIds: string[];
    unsavedContainers: string[];
    onAddRuleContainer: (container: string) => void;
    onRemoveRuleContainers: (containers: string[]) => void;
}

type Props = StateToProps & DispatchToProps & ContainerRuleContainersListProps

interface State {
    entitySaved: boolean;
}

class RuleContainerContainersList extends BaseComponent<Props, State> {

    constructor(props: Props) {
        super(props);
        this.state = {entitySaved: !this.isNew()};
    }

    public componentDidMount(): void {
        this.props.loadContainers();
        this.loadEntities();
    }

    public componentDidUpdate(prevProps: Readonly<Props>, prevState: Readonly<State>, snapshot?: any): void {
        if (!prevProps.ruleContainer?.name && this.props.ruleContainer?.name) {
            this.setState({entitySaved: true});
        }
    }

    public render() {
        const isNew = this.isNew();
        return <ControlledList
            isLoading={!isNew ? this.props.isLoadingRuleContainer || this.props.isLoading : undefined}
            error={this.props.loadRuleContainerError || (isNew ? undefined : this.props.error)}
            emptyMessage={`Sem contentores associados`}
            data={this.props.ruleContainers}
            dropdown={{
                id: 'containers',
                title: 'Selecionar o contentor',
                empty: 'Não existem contentor disponíveis',
                data: this.getSelectableContainers()
            }}
            show={this.container}
            onAdd={this.onAdd}
            onRemove={this.onRemove}
            onDelete={{
                url: `rules/containers/${this.props.ruleContainer?.name}/containers`,
                successCallback: this.onDeleteSuccess,
                failureCallback: this.onDeleteFailure
            }}
            entitySaved={this.state.entitySaved}
            invalidate={this.invalidate}/>;
    }

    private loadEntities = () => {
        if (this.props.ruleContainer?.name) {
            const {name} = this.props.ruleContainer;
            this.props.loadRuleContainers(name);
        }
    };

    private isNew = () =>
        this.props.ruleContainer?.name === undefined;

    private container = (index: number, container: string, separate: boolean, checked: boolean,
                         handleCheckbox: (event: React.ChangeEvent<HTMLInputElement>) => void): JSX.Element => {
        const isNew = this.isNew();
        const unsaved = this.props.unsavedContainersIds.includes(container);
        return (
            <ListItem key={index} separate={separate}>
                <div className={styles.linkedItemContent}>
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
        this.props.onAddRuleContainer(container);

    private onRemove = (containers: string[]) =>
        this.props.onRemoveRuleContainers(containers);

    private onDeleteSuccess = (containers: string[]): void => {
        if (this.props.ruleContainer?.name) {
            const {name} = this.props.ruleContainer;
            this.props.removeRuleContainers(name, containers);
        }
    };

    private onDeleteFailure = (reason: string): void =>
        super.toast(`Não foi possível remover o contentor`, 10000, reason, true);

    private getSelectableContainers = () => {
        const {containers, ruleContainers, unsavedContainersIds} = this.props;
        return Object.entries(containers).filter(([containerId, container]) =>
            (container.labels['serviceType'] === 'FRONTEND' || container.labels['serviceType'] === 'BACKEND')
            && !ruleContainers.includes(containerId)
            && !unsavedContainersIds.includes(containerId))
            .map(([_, container]) => container.name.replace('/', '') + " - " + container.id);
    };

    private invalidate = (data: string): string | undefined =>
        this.props.unsavedContainers.find(container => container.split(" - ")[1] === data)

}

function mapStateToProps(state: ReduxState, ownProps: ContainerRuleContainersListProps): StateToProps {
    const ruleName = ownProps.ruleContainer?.name;
    const rule = ruleName && state.entities.rules.containers.data[ruleName];
    const ruleContainers = rule && rule.containers;
    return {
        isLoading: state.entities.rules.containers.isLoadingContainers,
        error: state.entities.rules.containers.loadContainersError,
        ruleContainers: ruleContainers || [],
        containers: state.entities.containers.data,
    }
}

const mapDispatchToProps = (dispatch: any): DispatchToProps =>
    bindActionCreators({
        loadRuleContainers,
        removeRuleContainers,
        loadContainers,
    }, dispatch);

export default connect(mapStateToProps, mapDispatchToProps)(RuleContainerContainersList);