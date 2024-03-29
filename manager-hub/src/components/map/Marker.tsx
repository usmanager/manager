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

import {Point} from "react-simple-maps";
import React from "react";
import {ICoordinates} from "./LocationMap";

export interface IMarker extends ICoordinates {
    title: string,
    color?: string,
    titleCoordinates?: boolean,
}

interface Props {
    setTooltipContent: (tooltip: string) => void;
    title: string;
    titleCoordinates: boolean;
    label?: string;
    location: Point;
    color: string;
    size: number;
    onRemove?: () => void;
}

export default function Marker({setTooltipContent, title, titleCoordinates, label, location, color, size, onRemove}: Props) {
    let tooltip = "";
    if (title) {
        tooltip += title;
        if (titleCoordinates) {
            tooltip += ' (';
        }
    }
    if (titleCoordinates) {
        tooltip += `Lat: ${location[1].toFixed(2)}, Lon: ${location[0].toFixed(2)}`;
        if (title) {
            tooltip += ')';
        }
    }
    return (
        <>
            <circle r={size}
                    fill={color}
                    onMouseEnter={() => setTooltipContent(tooltip)}
                    onMouseLeave={() => setTooltipContent("")}
                    onClick={onRemove}/>
            <circle r={size} stroke='black' strokeWidth={size / 2.5} fill='none'/>
            {label &&
            <text
                style={{fontFamily: "system-ui", fill: "#000", fontSize: size * 3, pointerEvents: "none"}}
                textAnchor='middle'
                y={size * 3.7}>
                {label}
            </text>}
        </>

    );
}
