import React, { useState } from 'react';
import Select from './Select';
import { useTranslations } from 'next-intl';

export interface LogUpdateData {
    requestStatus: string;
    content: string;
}

interface RequestStatusAndResponseProps {
    initialStatusValue: string;
    onSave: (data: LogUpdateData) => void;
    unactive: boolean;
    isSubmitting: boolean;
}

const RequestLogUpdate = ({ initialStatusValue, onSave, unactive, isSubmitting }: RequestStatusAndResponseProps) => {
    const t = useTranslations('customer-interaction.Request');

    const [selectedStatus, setSelectedStatus] = useState<string>(initialStatusValue);
    const [content, setContent] = useState('');

    const handleSave = async () => {
        const trimmed = content.trim();
        if (!trimmed) {
            return;
        }
        const data: LogUpdateData = {
            requestStatus: selectedStatus,
            content: trimmed,
        };
        try {
            await onSave(data);
            handleCancel();
        } catch (error) {
            console.error('Save failed:', error);
        }
    };

    const handleCancel = () => {
        setSelectedStatus(initialStatusValue);
        setContent('');
    };

    if (unactive) {
        return (
            <div className="bg-gray-100 p-6 rounded-lg border border-gray-200 text-center text-gray-500 italic">
                <p>{t('formInactiveMessage')}</p>
            </div>
        );
    }

    return (
        <div className="bg-white p-6 rounded-lg shadow-sm border border-gray-200">
            <div className="flex justify-between items-center mb-4">
                <h3 className="text-lg font-semibold text-gray-800">{t('status')}</h3>
                <Select
                    options={[
                        { name: 'Pending', value: 'Pending' },
                        { name: 'Processing', value: 'Processing' },
                        { name: 'Done', value: 'Done' },
                    ]}
                    value={selectedStatus}
                    onSelect={(item) => setSelectedStatus(item.value)}
                    renderItem={(item) => item.name}
                    getValue={(item) => item.value}
                    placeholder={t('status')}
                />
            </div>

            <div>
                <h3 className="text-lg font-semibold text-gray-800 mb-2">{t('response')}</h3>
                <textarea
                    rows={5}
                    value={content}
                    onChange={(e) => setContent(e.target.value)}
                    className="w-full p-3 border border-gray-300 rounded-md focus:ring-green-500 focus:border-green-500"
                    placeholder={t('responsePlaceholder')}
                ></textarea>
            </div>

            <div className="flex justify-end space-x-3 mt-4">
                <button
                    onClick={handleCancel}
                    disabled={isSubmitting}
                    className="px-4 py-2 text-gray-700 border border-gray-300 rounded-lg hover:bg-gray-100 transition"
                >
                    {t('clear')}
                </button>
                <button
                    onClick={handleSave}
                    disabled={isSubmitting || content.trim().length === 0}
                    className="px-4 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700 transition shadow-md disabled:opacity-50 disabled:cursor-not-allowed"
                >
                    {t('save')}
                </button>
            </div>
        </div>
    );
};

export default RequestLogUpdate;
