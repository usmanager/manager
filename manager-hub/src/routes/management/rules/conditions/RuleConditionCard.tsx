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
import {deleteCondition} from "../../../../actions";
import {IRuleCondition} from "./RuleCondition";
import BaseComponent from "../../../../components/BaseComponent";
import {connect} from "react-redux";

interface State {
    loading: boolean;
}

interface ConditionCardProps {
    condition: IRuleCondition;
}

interface DispatchToProps {
    deleteCondition: (condition: IRuleCondition) => void;
}

type Props = DispatchToProps & ConditionCardProps;

class RuleConditionCard extends BaseComponent<Props, State> {

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

    private onDeleteSuccess = (condition: IRuleCondition): void => {
        super.toast(`<span class="green-text">A condição <b>${condition.name}</b> foi apagada com sucesso</span>`);
        if (this.mounted) {
            this.setState({loading: false});
        }
        this.props.deleteCondition(condition);
    }

    private onDeleteFailure = (reason: string, condition: IRuleCondition): void => {
        super.toast(`Não foi possível remover a condição <a href='/regras/condições/${condition.name}'><b>${condition.name}</b></a>`, 10000, reason, true);
        if (this.mounted) {
            this.setState({loading: false});
        }
    }

    public render() {
        const {condition} = this.props;
        const {loading} = this.state;
        const CardRuleCondition = Card<IRuleCondition>();
        return <CardRuleCondition id={`condition-${condition.id}`}
                                  title={condition.name.toString()}
                                  link={{to: {pathname: `/regras/condições/${condition.name}`, state: condition}}}
                                  height={'150px'}
                                  margin={'10px 0'}
                                  hoverable
                                  delete={{
                                      url: `rules/conditions/${condition.name}`,
                                      successCallback: this.onDeleteSuccess,
                                      failureCallback: this.onDeleteFailure,
                                  }}
                                  loading={loading}>
            <CardItem key={'valueMode'}
                      label={'Value mode'}
                      value={condition.valueMode.name}/>
            <CardItem key={'field'}
                      label={'Field'}
                      value={condition.field.name}/>
            <CardItem key={'operator'}
                      label={'Operator'}
                      value={condition.operator.symbol}/>
            <CardItem key={'value'}
                      label={'Value'}
                      value={condition.value.toString()}/>
        </CardRuleCondition>
    }
}

const mapDispatchToProps: DispatchToProps = {
    deleteCondition,
};

export default connect(null, mapDispatchToProps)(RuleConditionCard);
