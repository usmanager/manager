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

import {IRuleContainer} from "./RuleContainer";
import BaseComponent from "../../../../components/BaseComponent";
import React from "react";
import RuleContainerCard from "./RuleContainerCard";
import CardList from "../../../../components/list/CardList";
import {ReduxState} from "../../../../reducers";
import {connect} from "react-redux";
import {loadRulesContainer} from "../../../../actions";

interface StateToProps {
    isLoading: boolean
    error?: string | null;
    containerRules: IRuleContainer[];
}

interface DispatchToProps {
    loadRulesContainer: () => any;
}

type Props = StateToProps & DispatchToProps;

class RulesContainerList extends BaseComponent<Props, {}> {

    public componentDidMount(): void {
        this.props.loadRulesContainer();
    }

    public render() {
        return (
            <CardList<IRuleContainer>
                isLoading={this.props.isLoading}
                error={this.props.error}
                emptyMessage={"Não estão definidas regras aplicadas a contentores"}
                list={this.props.containerRules}
                card={this.rule}
                predicate={this.predicate}/>
        );
    }

    private rule = (rule: IRuleContainer): JSX.Element =>
        <RuleContainerCard key={rule.id} rule={rule}/>;

    private predicate = (rule: IRuleContainer, search: string): boolean =>
        rule.name.toLowerCase().includes(search);

}

const mapStateToProps = (state: ReduxState): StateToProps => (
    {
        isLoading: state.entities.rules.containers.isLoadingRules,
        error: state.entities.rules.containers.loadRulesError,
        containerRules: (state.entities.rules.containers.data && Object.values(state.entities.rules.containers.data).reverse()) || []
    }
);

const mapDispatchToProps: DispatchToProps = {
    loadRulesContainer,
};

export default connect(mapStateToProps, mapDispatchToProps)(RulesContainerList);