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

import {componentTypes, IDecision, IRule} from "../Rule";
import {RouteComponentProps} from "react-router";
import BaseComponent from "../../../../components/BaseComponent";
import Form, {IFields, requiredAndNumberAndMinAndMax, requiredAndTrimmed} from "../../../../components/form/Form";
import LoadingSpinner from "../../../../components/list/LoadingSpinner";
import {Error} from "../../../../components/errors/Error";
import Field from "../../../../components/form/Field";
import Tabs, {Tab} from "../../../../components/tabs/Tabs";
import MainLayout from "../../../../views/mainLayout/MainLayout";
import {ReduxState} from "../../../../reducers";
import {
    addRuleContainer,
    addRuleContainerConditions,
    addRuleContainers,
    loadDecisions,
    loadRulesContainer,
    updateRuleContainer,
} from "../../../../actions";
import {connect} from "react-redux";
import React from "react";
import {IReply, postData} from "../../../../utils/api";
import RuleContainerConditionList from "./RuleContainerConditionList";
import UnsavedChanged from "../../../../components/form/UnsavedChanges";
import RuleContainerContainersList from "./RuleContainerContainersList";
import {isNew} from "../../../../utils/router";
import {normalize} from "normalizr";
import {Schemas} from "../../../../middleware/api";

export interface IRuleContainer extends IRule {
    containers?: string[]
}

const buildNewContainerRule = (): Partial<IRuleContainer> => ({
    name: undefined,
    priority: 0,
    decision: undefined,
});

interface StateToProps {
    isLoading: boolean;
    error?: string | null;
    ruleContainer: Partial<IRuleContainer>;
    formRuleContainer?: Partial<IRuleContainer>,
    decisions: { [key: string]: IDecision },
}

interface DispatchToProps {
    loadRulesContainer: (name: string) => void;
    addRuleContainer: (ruleContainer: IRuleContainer) => void;
    updateRuleContainer: (previousRuleContainer: IRuleContainer, currentRuleContainer: IRuleContainer) => void;
    loadDecisions: () => void;
    addRuleContainerConditions: (ruleName: string, conditions: string[]) => void;
    addRuleContainers: (ruleName: string, containers: string[]) => void;
}

interface MatchParams {
    name: string;
}

interface LocationState {
    data: IRuleContainer,
    selected: 'rule' | 'ruleConditions' | 'containers'
}

type Props = StateToProps & DispatchToProps & RouteComponentProps<MatchParams, {}, LocationState>;

type State = {
    ruleContainer?: IRuleContainer,
    formRuleContainer?: IRuleContainer,
    unsavedConditions: string[],
    unsavedContainersIds: string[],
    unsavedContainers: string[],
}

class RuleContainer extends BaseComponent<Props, State> {

    state: State = {
        unsavedConditions: [],
        unsavedContainersIds: [],
        unsavedContainers: [],
    };
    private mounted = false;

    public componentDidMount(): void {
        this.loadRuleContainer();
        this.props.loadDecisions();
        this.mounted = true;
    };

    public componentWillUnmount(): void {
        this.mounted = false;
    }

    public render() {
        return (
            <MainLayout>
                {this.shouldShowSaveButton() && !isNew(this.props.location.search) && <UnsavedChanged/>}
                <div className="container">
                    <Tabs {...this.props} tabs={this.tabs()}/>
                </div>
            </MainLayout>
        );
    }

    private loadRuleContainer = () => {
        if (!isNew(this.props.location.search)) {
            const ruleName = this.props.match.params.name;
            this.props.loadRulesContainer(ruleName);
        }
    };

    private getRuleContainer = () =>
        this.state.ruleContainer || this.props.ruleContainer;

    private getFormRuleContainer = () =>
        this.state.formRuleContainer || this.props.formRuleContainer;

    private isNew = () =>
        isNew(this.props.location.search);

    private onPostSuccess = (reply: IReply<IRuleContainer>): void => {
        const ruleContainer = reply.data;
        super.toast(`<span class="green-text">A regra ${this.mounted ? `<b>${ruleContainer.name}</b>` : `<a href='/regras/contentores/${ruleContainer.name}'><b>${ruleContainer.name}</b></a>`} foi guardada com sucesso</span>`);
        this.props.addRuleContainer(ruleContainer);
        this.saveEntities(reply.data);
        if (this.mounted) {
            this.updateRuleContainer(ruleContainer);
            this.props.history.replace(ruleContainer.name);
        }
    };

    private onPostFailure = (reason: string, ruleContainer: IRuleContainer): void =>
        super.toast(`Não foi possível guardar a regra <b>${ruleContainer.name}</b>`, 10000, reason, true);

    private onPutSuccess = (reply: IReply<IRuleContainer>): void => {
        const ruleContainer = reply.data;
        super.toast(`<span class="green-text">As alterações à regra ${this.mounted ? `<b>${ruleContainer.name}</b>` : `<a href='/regras/contentores/${ruleContainer.name}'><b>${ruleContainer.name}</b></a>`} foram guardadas com sucesso</span>`);
        this.saveEntities(ruleContainer);
        const previousRuleContainer = this.getRuleContainer();
        if (previousRuleContainer?.id) {
            this.props.updateRuleContainer(previousRuleContainer as IRuleContainer, ruleContainer)
        }
        if (this.mounted) {
            this.updateRuleContainer(ruleContainer);
            this.props.history.replace(ruleContainer.name);
        }
    };

    private onPutFailure = (reason: string, ruleContainer: IRuleContainer): void =>
        super.toast(`Não foi possível atualizar a regra ${this.mounted ? `<b>${ruleContainer.name}</b>` : `<a href='/regras/contentores/${ruleContainer.name}><b>${ruleContainer.name}'</b></a>`}`, 10000, reason, true);

    private onDeleteSuccess = (ruleContainer: IRuleContainer): void => {
        super.toast(`<span class="green-text">A regra <b>${ruleContainer.name}</b> foi apagada com sucesso</span>`);
        if (this.mounted) {
            this.props.history.push(`/regras/contentores`);
        }
    };

    private onDeleteFailure = (reason: string, ruleContainer: IRuleContainer): void =>
        super.toast(`Não foi possível remover container rule ${this.mounted ? `<b>${ruleContainer.name}</b>` : `<a href='/regras/containers/${ruleContainer.name}><b>${ruleContainer.name}'</b></a>`}`, 10000, reason, true);

    private shouldShowSaveButton = () =>
        !!this.state.unsavedConditions.length
        || !!this.state.unsavedContainersIds.length;

    private saveEntities = (rule: IRuleContainer) => {
        this.saveRuleConditions(rule);
        this.saveRuleContainers(rule);
    };

    private addRuleCondition = (condition: string): void => {
        this.setState({
            unsavedConditions: this.state.unsavedConditions.concat(condition)
        });
    };

    private removeRuleConditions = (conditions: string[]): void => {
        this.setState({
            unsavedConditions: this.state.unsavedConditions.filter(condition => !conditions.includes(condition))
        });
    };

    private saveRuleConditions = (rule: IRuleContainer): void => {
        const {unsavedConditions} = this.state;
        if (unsavedConditions.length) {
            postData(`rules/containers/${rule.name}/conditions`, unsavedConditions,
                () => this.onSaveConditionsSuccess(rule),
                (reason) => this.onSaveConditionsFailure(rule, reason));
        }
    };

    private onSaveConditionsSuccess = (rule: IRuleContainer): void => {
        this.props.addRuleContainerConditions(rule.name, this.state.unsavedConditions);
        if (this.mounted) {
            this.setState({unsavedConditions: []});
        }
    };

    private onSaveConditionsFailure = (ruleContainer: IRuleContainer, reason: string): void =>
        super.toast(`Não foi possível guardar as condições associadas à regra ${this.mounted ? `<b>${ruleContainer.name}</b>` : `<a href='/regras/containers/${ruleContainer.name}'><b>${ruleContainer.name}</b></a>`}`, 10000, reason, true);

    private addRuleContainer = (container: string): void => {
        const containerId = container.split(" - ")[1]
        this.setState({
            unsavedContainersIds: this.state.unsavedContainersIds.concat(containerId),
            unsavedContainers: this.state.unsavedContainers.concat(container)
        });
    };

    private removeRuleContainers = (containers: string[]): void => {
        const containersIds = containers.map(container => container.split(" - ")[1])
        this.setState({
            unsavedContainersIds: this.state.unsavedContainersIds.filter(container => !containersIds.includes(container)),
            unsavedContainers: this.state.unsavedContainers.filter(container => !containers.includes(container))
        });
    };

    private saveRuleContainers = (rule: IRuleContainer): void => {
        const {unsavedContainersIds} = this.state;
        if (unsavedContainersIds.length) {
            postData(`rules/containers/${rule.name}/containers`, unsavedContainersIds,
                () => this.onSaveContainersSuccess(rule),
                (reason) => this.onSaveContainersFailure(rule, reason));
        }
    };

    private onSaveContainersSuccess = (rule: IRuleContainer): void => {
        this.props.addRuleContainers(rule.name, this.state.unsavedContainersIds);
        if (this.mounted) {
            this.setState({unsavedContainersIds: []});
        }
    };

    private onSaveContainersFailure = (ruleContainer: IRuleContainer, reason: string): void =>
        super.toast(`Não foi possível guardar os contentores associados à regra ${this.mounted ? `<b>${ruleContainer.name}</b>` : `<a href='/regras/containers/${ruleContainer.name}'><b>${ruleContainer.name}</b></a>`}`, 10000, reason, true);

    private updateRuleContainer = (ruleContainer: IRuleContainer) => {
        ruleContainer = Object.values(normalize(ruleContainer, Schemas.RULE_CONTAINER).entities.containerRules || {})[0];
        const formRuleContainer = {...ruleContainer};
        removeFields(formRuleContainer);
        this.setState({ruleContainer: ruleContainer, formRuleContainer: formRuleContainer});
    };

    private getFields = (containerRule: Partial<IRule>): IFields =>
        Object.entries(containerRule).map(([key, _]) => {
            return {
                [key]: {
                    id: key,
                    label: key,
                    validation:
                        key === 'priority'
                            ? {rule: requiredAndNumberAndMinAndMax, args: [0, 2147483647]}
                            : key !== 'generic'
                            ? {rule: requiredAndTrimmed}
                            : undefined
                }
            };
        }).reduce((fields, field) => {
            for (let key in field) {
                fields[key] = field[key];
            }
            return fields;
        }, {});

    private decisionDropdownOption = (decision: IDecision): string =>
        decision.ruleDecision;

    private getSelectableDecisions = () =>
        Object.values(this.props.decisions)
            .filter(decision => decision.componentType.type.toLowerCase() === componentTypes.SERVICE.type.toLowerCase());

    private containerRule = () => {
        const {isLoading, error} = this.props;
        const ruleContainer = this.getRuleContainer();
        const formRuleContainer = this.getFormRuleContainer();
        // @ts-ignore
        const ruleKey: (keyof IRuleContainer) = formRuleContainer && Object.keys(formRuleContainer)[0];
        const isNewRuleContainer = this.isNew();
        return (
            <>
                {isLoading && <LoadingSpinner/>}
                {!isLoading && error && <Error message={error}/>}
                {!isLoading && !error && formRuleContainer && (
                    /*@ts-ignore*/
                    <Form id={ruleKey}
                          fields={this.getFields(formRuleContainer)}
                          values={ruleContainer}
                          isNew={isNew(this.props.location.search)}
                          showSaveButton={this.shouldShowSaveButton()}
                          post={{
                              url: 'rules/containers',
                              successCallback: this.onPostSuccess,
                              failureCallback: this.onPostFailure
                          }}
                          put={{
                              url: `rules/containers/${ruleContainer.name}`,
                              successCallback: this.onPutSuccess,
                              failureCallback: this.onPutFailure
                          }}
                          delete={{
                              url: `rules/containers/${ruleContainer.name}`,
                              successCallback: this.onDeleteSuccess,
                              failureCallback: this.onDeleteFailure
                          }}
                          saveEntities={this.saveEntities}>
                        {Object.entries(formRuleContainer).map(([key, value], index) =>
                            key === 'decision'
                                ? <Field<IDecision> key={index}
                                                    id={key}
                                                    label={key}
                                                    type="dropdown"
                                                    dropdown={{
                                                        defaultValue: "Selecionar a decisão",
                                                        values: this.getSelectableDecisions(),
                                                        optionToString: this.decisionDropdownOption,
                                                        emptyMessage: 'Não existem decisões disponíveis'
                                                    }}/>
                                : <Field key={index}
                                         id={key}
                                         label={key}
                                         type={key === 'priority' ? "number" : "text"}/>
                        )}
                    </Form>
                )}
            </>
        )
    };

    private conditions = (): JSX.Element =>
        <RuleContainerConditionList isLoadingRuleContainer={this.props.isLoading}
                                    loadRuleContainerError={this.props.error}
                                    ruleContainer={this.getRuleContainer()}
                                    unsavedConditions={this.state.unsavedConditions}
                                    onAddRuleCondition={this.addRuleCondition}
                                    onRemoveRuleConditions={this.removeRuleConditions}/>;

    private containers = (): JSX.Element =>
        <RuleContainerContainersList isLoadingRuleContainer={this.props.isLoading}
                                     loadRuleContainerError={this.props.error}
                                     ruleContainer={this.getRuleContainer()}
                                     unsavedContainersIds={this.state.unsavedContainersIds}
                                     unsavedContainers={this.state.unsavedContainers}
                                     onAddRuleContainer={this.addRuleContainer}
                                     onRemoveRuleContainers={this.removeRuleContainers}/>;

    private tabs = (): Tab[] => [
        {
            title: 'Regra',
            id: 'containerRule',
            content: () => this.containerRule(),
            active: this.props.location.state?.selected === 'rule'
        },
        {
            title: 'Condições',
            id: 'ruleConditions',
            content: () => this.conditions(),
            active: this.props.location.state?.selected === 'ruleConditions'
        },
        {
            title: 'Contentores',
            id: 'containers',
            content: () => this.containers(),
            active: this.props.location.state?.selected === 'containers'
        }
    ];

}

function removeFields(ruleContainer: Partial<IRuleContainer>) {
    delete ruleContainer["id"];
    delete ruleContainer["conditions"];
    delete ruleContainer["containers"];
}

function mapStateToProps(state: ReduxState, props: Props): StateToProps {
    const isLoading = state.entities.rules.containers.isLoadingRules;
    const error = state.entities.rules.containers.loadRulesError;
    const name = props.match.params.name;
    const ruleContainer = isNew(props.location.search) ? buildNewContainerRule() : state.entities.rules.containers.data[name];
    let formRuleContainer;
    if (ruleContainer) {
        formRuleContainer = {...ruleContainer};
        removeFields(formRuleContainer);
    }
    const decisions = state.entities.decisions.data;
    return {
        isLoading,
        error,
        ruleContainer,
        formRuleContainer,
        decisions,
    }
}

const mapDispatchToProps: DispatchToProps = {
    loadRulesContainer,
    addRuleContainer,
    updateRuleContainer,
    loadDecisions,
    addRuleContainerConditions,
    addRuleContainers,
};

export default connect(mapStateToProps, mapDispatchToProps)(RuleContainer);
