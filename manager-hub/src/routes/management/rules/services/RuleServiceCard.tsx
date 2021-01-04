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

import Card from "../../../../components/cards/Card";
import CardItem from "../../../../components/list/CardItem";
import React from "react";
import {deleteServiceRule} from "../../../../actions";
import BaseComponent from "../../../../components/BaseComponent";
import LinkedContextMenuItem from "../../../../components/contextmenu/LinkedContextMenuItem";
import {connect} from "react-redux";
import {IRuleService} from "./RuleService";

interface State {
    loading: boolean;
}

interface ServiceCardProps {
    rule: IRuleService;
}

interface DispatchToProps {
    deleteServiceRule: (serviceRule: IRuleService) => void;
}

type Props = DispatchToProps & ServiceCardProps;

class RuleServiceCard extends BaseComponent<Props, State> {

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

    private onDeleteSuccess = (ruleService: IRuleService): void => {
        super.toast(`<span class="green-text">A regra <b>${ruleService.name}</b> foi apagada com sucesso</span>`);
        if (this.mounted) {
            this.setState({loading: false});
        }
        this.props.deleteServiceRule(ruleService);
    }

    private onDeleteFailure = (reason: string, ruleService: IRuleService): void => {
        super.toast(`Não foi possível remover a regra <a href='/regras/serviços/${ruleService.name}'><b>${ruleService.name}</b></a>`, 10000, reason, true);
        if (this.mounted) {
            this.setState({loading: false});
        }
    }

    private contextMenu = (): JSX.Element[] => {
        const {rule} = this.props;
        const menuItems = [
            <LinkedContextMenuItem
                option={'Modificar as condições associadas'}
                pathname={`/regras/serviços/${rule.name}`}
                selected={'ruleConditions'}
                state={rule}/>
        ];
        if (!rule.generic) {
            menuItems.push(
                <LinkedContextMenuItem
                    option={'Modify services'}
                    pathname={`/regras/serviços/${rule.name}`}
                    selected={'services'}
                    state={rule}/>
            );
        }
        return menuItems;
    }

    public render() {
        const {rule} = this.props;
        const {loading} = this.state;
        const CardRuleService = Card<IRuleService>();
        return <CardRuleService id={`service-rule-${rule.id}`}
                                title={rule.name}
                                link={{to: {pathname: `/regras/serviços/${rule.name}`, state: rule}}}
                                height={'120px'}
                                margin={'10px 0'}
                                hoverable
                                delete={{
                                    url: `rules/services/${rule.name}`,
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
            <CardItem key={'generic'}
                      label={'Generic'}
                      value={`${rule.generic}`}/>
        </CardRuleService>
    }
}

const mapDispatchToProps: DispatchToProps = {
    deleteServiceRule,
};

export default connect(null, mapDispatchToProps)(RuleServiceCard);
