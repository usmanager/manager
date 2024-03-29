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
import {ISimulatedHostMetric} from "./SimulatedHostMetric";
import BaseComponent from "../../../../components/BaseComponent";
import LinkedContextMenuItem from "../../../../components/contextmenu/LinkedContextMenuItem";
import {deleteSimulatedHostMetric} from "../../../../actions";
import {connect} from "react-redux";

interface State {
    loading: boolean;
}

interface SimulatedHostMetricCardProps {
    simulatedHostMetric: ISimulatedHostMetric;
}

interface DispatchToProps {
    deleteSimulatedHostMetric: (simulatedHostMetric: ISimulatedHostMetric) => void;
}

type Props = DispatchToProps & SimulatedHostMetricCardProps;

class SimulatedHostMetricCard extends BaseComponent<Props, State> {

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

    private onDeleteSuccess = (simulatedMetric: ISimulatedHostMetric): void => {
        super.toast(`<span class="green-text">A métrica simulada <b>${simulatedMetric.name}</b> foi apagada com sucesso</span>`);
        if (this.mounted) {
            this.setState({loading: false});
        }
        this.props.deleteSimulatedHostMetric(simulatedMetric);
    }

    private onDeleteFailure = (reason: string, simulatedMetric: ISimulatedHostMetric): void => {
        super.toast(`Não foi possível remover a métrica simulada <a href='/métricas simuladas/hosts/${simulatedMetric.name}'><b>${simulatedMetric.name}</b></a>`, 10000, reason, true);
        if (this.mounted) {
            this.setState({loading: false});
        }
    }

    private contextMenu = (): JSX.Element[] => {
        const {simulatedHostMetric} = this.props;
        return [
            <LinkedContextMenuItem
                option={'Alterar a lista de instâncias cloud'}
                pathname={`/métricas simuladas/hosts/${simulatedHostMetric.name}`}
                selected={'cloudHosts'}
                state={simulatedHostMetric}/>,
            <LinkedContextMenuItem
                option={'Alterar a lista de edge hosts'}
                pathname={`/métricas simuladas/hosts/${simulatedHostMetric.name}`}
                selected={'edgeHosts'}
                state={simulatedHostMetric}/>,
        ];
    }

    public render() {
        const {simulatedHostMetric} = this.props;
        const {loading} = this.state;
        const CardSimulatedHostMetric = Card<ISimulatedHostMetric>();
        return <CardSimulatedHostMetric id={`simulated-host-metric-${simulatedHostMetric.id}`}
                                        title={simulatedHostMetric.name}
                                        link={{
                                            to: {
                                                pathname: `/métricas simuladas/hosts/${simulatedHostMetric.name}`,
                                                state: simulatedHostMetric
                                            }
                                        }}
                                        height={'212px'}
                                        margin={'10px 0'}
                                        hoverable
                                        delete={{
                                            url: `simulated-metrics/hosts/${simulatedHostMetric.name}`,
                                            successCallback: this.onDeleteSuccess,
                                            failureCallback: this.onDeleteFailure,
                                        }}
                                        loading={loading}
                                        bottomContextMenuItems={this.contextMenu()}>
            <CardItem key={'Field'}
                      label={'Field'}
                      value={`${simulatedHostMetric.field.name}`}/>
            <CardItem key={'MinimumValue'}
                      label='Minimum value'
                      value={`${simulatedHostMetric.minimumValue}`}/>
            <CardItem key={'MaximumValue'}
                      label='Maximum value'
                      value={`${simulatedHostMetric.maximumValue}`}/>
            <CardItem key={'Override'}
                      label='Override'
                      value={`${simulatedHostMetric.override}`}/>
            <CardItem key={'Generic'}
                      label='Generic'
                      value={`${simulatedHostMetric.generic}`}/>
            <CardItem key={'Active'}
                      label='Active'
                      value={`${simulatedHostMetric.active}`}/>
        </CardSimulatedHostMetric>
    }
}

const mapDispatchToProps: DispatchToProps = {
    deleteSimulatedHostMetric,
};

export default connect(null, mapDispatchToProps)(SimulatedHostMetricCard);
