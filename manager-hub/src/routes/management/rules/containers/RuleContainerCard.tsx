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
 * furnished to do so, subject to the following containerRules:
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

import Card from "../../../../components/cards/Card";
import CardItem from "../../../../components/list/CardItem";
import React from "react";
import {deleteContainerRule} from "../../../../actions";
import {IRuleContainer} from "./RuleContainer";
import BaseComponent from "../../../../components/BaseComponent";
import LinkedContextMenuItem from "../../../../components/contextmenu/LinkedContextMenuItem";
import {connect} from "react-redux";

interface State {
    loading: boolean;
}

interface ContainerCardProps {
    rule: IRuleContainer;
}

interface DispatchToProps {
    deleteContainerRule: (containerRule: IRuleContainer) => void;
}

type Props = DispatchToProps & ContainerCardProps;

class RuleContainerCard extends BaseComponent<Props, State> {

    private mounted = false;

    constructor(props: Props) {
        super(props);
        this.state = {
            loading: false
        }
    }

    public componentDidMount(): void {
        this.mounted = true;
    };

    public componentWillUnmount(): void {
        this.mounted = false;
    }

    private onDeleteSuccess = (ruleContainer: IRuleContainer): void => {
        super.toast(`<span class="green-text">A regra <b>${ruleContainer.name}</b> foi apagada com sucesso</span>`);
        if (this.mounted) {
            this.setState({loading: false});
        }
        this.props.deleteContainerRule(ruleContainer);
    }

    private onDeleteFailure = (reason: string, ruleContainer: IRuleContainer): void => {
        super.toast(`Não foi possível remover a regra <a href='/regras/contentores/${ruleContainer.name}'><b>${ruleContainer.name}</b></a>`, 10000, reason, true);
        if (this.mounted) {
            this.setState({loading: false});
        }
    }

    private contextMenu = (): JSX.Element[] => {
        const {rule} = this.props;
        return [
            <LinkedContextMenuItem
                option={'Modificar as condições associadas'}
                pathname={`/regras/contentores/${rule.name}`}
                selected={'ruleConditions'}
                state={rule}/>,
            <LinkedContextMenuItem
                option={'Modificar a lista de contentores'}
                pathname={`/regras/contentores/${rule.name}`}
                selected={'containers'}
                state={rule}/>
        ];
    }

    public render() {
        const {rule} = this.props;
        const {loading} = this.state;
        const CardRuleContainer = Card<IRuleContainer>();
        return <CardRuleContainer id={`container-rule-${rule.id}`}
                                  title={rule.name}
                                  link={{to: {pathname: `/regras/contentores/${rule.name}`, state: rule}}}
                                  height={'85px'}
                                  margin={'10px 0'}
                                  hoverable
                                  delete={{
                                      url: `rules/containers/${rule.name}`,
                                      successCallback: this.onDeleteSuccess,
                                      failureCallback: this.onDeleteFailure,
                                  }}
                                  loading={loading}
                                  bottomContextMenuItems={this.contextMenu()}>
            <CardItem key={'priority'}
                      label={'Priority'}
                      value={`${rule.priority}`}/>
            <CardItem key={'decision'}
                      label={'Decision'}
                      value={`${rule.decision.ruleDecision}`}/>
        </CardRuleContainer>
    }
}

const mapDispatchToProps: DispatchToProps = {
    deleteContainerRule,
};

export default connect(null, mapDispatchToProps)(RuleContainerCard);
