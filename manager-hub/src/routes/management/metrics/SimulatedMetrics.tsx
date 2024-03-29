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

import React from 'react';
import MainLayout from '../../../views/mainLayout/MainLayout';
import AddButton from "../../../components/form/AddButton";
import styles from './SimulatedMetrics.module.css'
import Collapsible from "../../../components/collapsible/Collapsible";
import SimulatedHostMetricsList from "./hosts/SimulatedHostMetricsList";
import SimulatedServiceMetricsList from "./services/SimulatedServiceMetricsList";
import SimulatedContainerMetricsList from "./containers/SimulatedContainerMetricsList";
import SimulatedAppMetricsList from "./apps/SimulatedAppMetricsList";

const SimulatedMetrics = () =>
    <MainLayout>
        <AddButton button={{text: 'Nova métrica simulada'}}
                   dropdown={{
                       id: 'simulatedMetrics',
                       title: 'Nova métrica simulada...',
                       data: [
                           {text: 'aplicada a hosts', pathname: '/métricas simuladas/hosts/nova métrica?new'},
                           {text: 'aplicada a aplicações', pathname: '/métricas simuladas/aplicações/nova métrica?new'},
                           {text: 'aplicada a serviços', pathname: '/métricas simuladas/serviços/nova métrica?new'},
                           {
                               text: 'aplicada a contentores',
                               pathname: '/métricas simuladas/contentores/nova métrica?new'
                           },
                       ],
                   }}/>
        <div className={`${styles.container}`}>
            <Collapsible id={"hostsCollapsible"}
                         title={'Hosts'}
                         active
                         headerClassname={styles.collapsibleSubtitle}
                         bodyClassname={styles.collapsibleCardList}>
                <SimulatedHostMetricsList/>
            </Collapsible>
        </div>
        <div className={`${styles.container}`}>
            <Collapsible id={"appsCollapsible"}
                         title={'Aplicações'}
                         active
                         headerClassname={styles.collapsibleSubtitle}
                         bodyClassname={styles.collapsibleCardList}>
                <SimulatedAppMetricsList/>
            </Collapsible>
        </div>
        <div className={`${styles.container}`}>
            <Collapsible id={"servicesCollapsible"}
                         title={'Serviços'}
                         active
                         headerClassname={styles.collapsibleSubtitle}
                         bodyClassname={styles.collapsibleCardList}>
                <SimulatedServiceMetricsList/>
            </Collapsible>
        </div>
        <div className={`${styles.container}`}>
            <Collapsible id={"containersCollapsible"}
                         title={'Contentores'}
                         active
                         headerClassname={styles.collapsibleSubtitle}
                         bodyClassname={styles.collapsibleCardList}>
                <SimulatedContainerMetricsList/>
            </Collapsible>
        </div>
    </MainLayout>;

export default SimulatedMetrics;