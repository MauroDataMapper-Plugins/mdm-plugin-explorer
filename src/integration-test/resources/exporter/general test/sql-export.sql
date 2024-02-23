SELECT
    [people].[patients].[id],
    [people].[patients].[forename],
    [people].[patients].[surname],
    [people].[patients].[dateOfBirth],
    [people].[patients].[age],
    [people].[patients].[height],
    [people].[patients].[exercisesRegularly]
FROM
    [people].[patients]
JOIN
    [medical].[episodes]
ON
    [medical].[episodes].[patientId] = [people].[patients].[id]
WHERE
    [people].[patients].[dateOfBirth] > convert(datetime, '01/01/1960', 103)
    AND (
        [medical].[episodes].[occurredAt] = convert(datetime, '25/12/2020', 103)
        AND [medical].[episodes].[score] IN (1, 2, 9, 10)
    )
    AND (
        [people].[patients].[age] > 18
        AND [people].[patients].[age] < 65
    )
GO

SELECT
    [medical].[episodes].[id],
    [medical].[episodes].[patientId],
    [medical].[episodes].[description],
    [medical].[episodes].[score],
    [medical].[episodes].[occurredAt],
    [medical].[episodes].[do_not_include]
FROM
    [medical].[episodes]
WHERE
    [medical].[episodes].[do_not_include] = 1
    OR [medical].[episodes].[do_not_include] = 0
GO

SELECT
    [medical].[treatments].[id],
    [medical].[treatments].[patientId],
    [medical].[treatments].[description],
    [medical].[treatments].[givenOn],
    [medical].[treatments].[do_not_include]
FROM
    [medical].[treatments]
WHERE
    [medical].[treatments].[givenOn] > convert(datetime, '01/01/2023', 103)
    AND [medical].[treatments].[givenOn] < convert(datetime, '31/12/2023', 103)
GO